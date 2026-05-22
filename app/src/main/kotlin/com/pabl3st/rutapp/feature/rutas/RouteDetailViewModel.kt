package com.pabl3st.rutapp.feature.rutas

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.location.LocationManager
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.StopTagConfig
import com.pabl3st.rutapp.data.local.entity.evaluateTag
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.AdminRepository
import com.pabl3st.rutapp.data.repository.AuthResult
import com.pabl3st.rutapp.data.network.AccountUserDto
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.repository.UserPrefsRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

enum class StopSortMode { MANUAL, GPS, GREEDY }

data class RouteDetailUiState(
    val route: RouteEntity?                       = null,
    val stops: List<StopEntity>                   = emptyList(),
    val sortMode: StopSortMode                    = StopSortMode.MANUAL,
    val isReordering: Boolean                     = false,
    val isLoading: Boolean                        = true,
    val error: String?                            = null,
    // Tags configurados por el owner — evaluados por stop en la UI
    val stopTags: List<StopTagConfig>             = emptyList(),
    // kpiId→value por stopUid — para evaluar condiciones KPI en tags
    val kpiByStop: Map<String, Map<String,String>> = emptyMap(),
    // Permisos — solo owner/admin pueden añadir/eliminar paradas
    val canEditStops: Boolean                     = false,
    // Reasignación de ruta
    val canReassign:        Boolean              = false,
    val showReassignDialog: Boolean              = false,
    val assignableUsers:    List<AccountUserDto> = emptyList(),
    val selectedAssigneeId: Int?                 = null,
    val loadingUsers:       Boolean              = false,
    val isReassigning:      Boolean              = false,
    val snackbar:           String?              = null,
    // Selector de fecha para rutas multi-día
    val availableDates: List<String>              = emptyList(),
    val selectedDate:   String?                   = java.time.LocalDate.now().toString(),
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepo:    RouteRepository,
    private val adminRepo:    AdminRepository,
    private val stopRepo:     StopRepository,
    private val locationMgr:  LocationManager,
    private val kpiValueDao:  KpiValueDao,
    private val prefsRepo:    UserPrefsRepository,
    private val session:      SessionManager,
) : BaseViewModel() {

    private val routeUid: String = checkNotNull(savedStateHandle["routeUid"])

    private val _ui = MutableStateFlow(RouteDetailUiState(
        canEditStops = session.userRole in listOf("owner", "admin", "god", "manager"),
        canReassign  = session.userRole in listOf("owner", "admin", "manager", "god"),
    ))
    val ui: StateFlow<RouteDetailUiState> = _ui.asStateFlow()

    // Lista base de Room (siempre por orderIndex)
    private val _baseStops = MutableStateFlow<List<StopEntity>>(emptyList())

    init {
        loadRoute()
        observeStops()
        observeKpiValues()
        observeTags()
    }

    private fun loadRoute() {
        viewModelScope.launch {
            // Fetch inicial con retry — la ruta puede llegar vía sync justo después del deeplink FCM
            var route = routeRepo.getByUid(routeUid)
            if (route == null) {
                // Esperar hasta 10s en intervalos de 1s (el sync ondemand suele tardar 2-4s)
                _ui.update { it.copy(isLoading = true) }
                repeat(10) { attempt ->
                    if (route != null) return@repeat
                    delay(1_000L)
                    route = routeRepo.getByUid(routeUid)
                }
            }
            _ui.update { it.copy(
                route    = route,
                isLoading = false,
                error    = if (route == null) "Ruta no encontrada. Comprueba tu conexión y vuelve a intentarlo." else null,
            ) }
        }
    }

    private fun observeStops() {
        viewModelScope.launch {
            // Cargar fechas disponibles y filtrar stops por fecha seleccionada
            val availDates = stopRepo.getDistinctDates(routeUid)
            if (availDates.isNotEmpty()) {
                _ui.update { it.copy(availableDates = availDates) }
                val sel = _ui.value.selectedDate ?: availDates.first()
                stopRepo.observeByRouteAndDate(routeUid, sel)
            } else {
                stopRepo.observeByRoute(routeUid)
            }
                .catch { e -> _ui.update { it.copy(error = e.message) } }
                .collect { stops ->
                    _baseStops.value = stops
                    applySortMode(_ui.value.sortMode, stops)
                }
        }
    }

    fun setSortMode(mode: StopSortMode) {
        _ui.update { it.copy(sortMode = mode) }
        viewModelScope.launch {
            applySortMode(mode, _baseStops.value)
        }
    }

    private suspend fun applySortMode(mode: StopSortMode, stops: List<StopEntity>) {
        val sorted = when (mode) {
            StopSortMode.MANUAL  -> stops
            StopSortMode.GPS     -> sortByGps(stops)
            StopSortMode.GREEDY  -> sortGreedy(stops)
        }
        _ui.update { it.copy(stops = sorted) }
    }

    // ── Nearest-neighbor desde posición GPS actual ────────────
    private suspend fun sortByGps(stops: List<StopEntity>): List<StopEntity> {
        val loc = locationMgr.getLastLocation() ?: return stops
        val withGps = stops.filter { it.lat != null && it.lng != null }
        val withoutGps = stops.filter { it.lat == null || it.lng == null }
        val sorted = withGps.sortedBy { haversine(loc.latitude, loc.longitude, it.lat!!, it.lng!!) }
        return sorted + withoutGps
    }

    // ── Greedy nearest-neighbor desde la primera parada ──────
    private fun sortGreedy(stops: List<StopEntity>): List<StopEntity> {
        val withGps = stops.filter { it.lat != null && it.lng != null }.toMutableList()
        val withoutGps = stops.filter { it.lat == null || it.lng == null }
        if (withGps.size <= 1) return stops

        val result = mutableListOf<StopEntity>()
        var current = withGps.removeAt(0)
        result.add(current)

        while (withGps.isNotEmpty()) {
            val next = withGps.minByOrNull { haversine(current.lat!!, current.lng!!, it.lat!!, it.lng!!) }!!
            withGps.remove(next)
            result.add(next)
            current = next
        }
        return result + withoutGps
    }

    // ── Persistir orden actual en Room ────────────────────────
    fun saveCurrentOrder() {
        viewModelScope.launch {
            _ui.update { it.copy(isReordering = true) }
            stopRepo.reorderStops(_ui.value.stops)
            _ui.update { it.copy(isReordering = false) }
        }
    }

    // markStopVisited eliminado — el marcado solo ocurre al guardar el formulario de visita

    private fun observeKpiValues() {
        viewModelScope.launch {
            _baseStops.flatMapLatest { stops ->
                val uids = stops.map { it.uid }
                if (uids.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
                else kpiValueDao.observeByStops(uids)
            }.collect { kpiList ->
                val byStop = kpiList
                    .groupBy { it.stopUid }
                    .mapValues { (_, vals) -> vals.associate { it.kpiId to it.valueText } }
                _ui.update { it.copy(kpiByStop = byStop) }
            }
        }
    }

    private fun observeTags() {
        viewModelScope.launch {
            prefsRepo.prefs.collect { prefs ->
                _ui.update { it.copy(stopTags = prefs.stopTags) }
            }
        }
    }

    // ── Reasignación ─────────────────────────────────────────
    fun onShowReassignDialog() {
        _ui.update { it.copy(showReassignDialog = true, selectedAssigneeId = null, assignableUsers = emptyList()) }
        loadAssignableUsers()
    }

    fun onDismissReassignDialog() =
        _ui.update { it.copy(showReassignDialog = false, selectedAssigneeId = null, assignableUsers = emptyList()) }

    fun onSelectAssignee(userId: Int?) = _ui.update { it.copy(selectedAssigneeId = userId) }

    fun clearSnackbar() = _ui.update { it.copy(snackbar = null) }

    private fun loadAssignableUsers() {
        viewModelScope.launch {
            _ui.update { it.copy(loadingUsers = true) }
            when (val r = adminRepo.listUsers()) {
                is AuthResult.Success -> {
                    val myId   = session.userId
                    val myRole = session.userRole
                    // Excluir al propietario actual de la ruta (no tiene sentido reasignar al mismo)
                    val currentOwnerId = _ui.value.route?.userId
                    val assignable = r.data.filter { u ->
                        u.isActive && u.userId != currentOwnerId && when (myRole) {
                            "god"     -> u.role in listOf("owner", "admin", "manager", "agent")
                            "owner"   -> u.role in listOf("admin", "manager", "agent")
                            "admin"   -> u.role in listOf("manager", "agent")
                            "manager" -> true  // servidor ya filtró solo los reportadores directos
                            else      -> false
                        }
                    }
                    _ui.update { it.copy(assignableUsers = assignable, loadingUsers = false) }
                }
                is AuthResult.Error -> _ui.update { it.copy(loadingUsers = false) }
            }
        }
    }

    fun confirmReassign() {
        val targetId = _ui.value.selectedAssigneeId ?: return
        val routeUid = routeUid
        viewModelScope.launch {
            _ui.update { it.copy(isReassigning = true) }
            routeRepo.reassignRoute(routeUid, targetId)
                .onSuccess {
                    val name = _ui.value.assignableUsers
                        .firstOrNull { it.userId == targetId }?.displayName ?: "usuario"
                    _ui.update { it.copy(
                        isReassigning      = false,
                        showReassignDialog = false,
                        selectedAssigneeId = null,
                        snackbar           = "Ruta reasignada a $name",
                    ) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isReassigning = false, error = e.message ?: "Error al reasignar") }
                }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }

    fun onDateSelected(date: String) {
        _ui.update { it.copy(selectedDate = date) }
        viewModelScope.launch {
            stopRepo.observeByRouteAndDate(routeUid, date)
                .collect { stops -> applySortMode(_ui.value.sortMode, stops) }
        }
    }

    // ── Haversine en km ───────────────────────────────────────
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(
            isLoading = false,
            error     = t.message ?: "Error inesperado",
        )}
    }

}



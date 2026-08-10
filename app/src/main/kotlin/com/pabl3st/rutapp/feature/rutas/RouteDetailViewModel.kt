package com.pabl3st.rutapp.feature.rutas

import com.pabl3st.rutapp.core.UserRole

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.location.LocationManager
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.StopTagConfig
import com.pabl3st.rutapp.data.local.entity.StopVisitEntity
import com.pabl3st.rutapp.data.local.entity.evaluateTag
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.AdminRepository
import com.pabl3st.rutapp.data.repository.AuthResult
import com.pabl3st.rutapp.data.network.AccountUserDto
import com.pabl3st.rutapp.data.network.RouteAssignmentDto
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.repository.StopVisitRepository
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
    val routeOwnerName:     String?              = null,  // nombre del agente asignado (si != caller)
    val showReassignDialog: Boolean              = false,
    val assignableUsers:    List<AccountUserDto> = emptyList(),
    val selectedAssigneeId: Int?                 = null,
    val loadingUsers:       Boolean              = false,
    val isReassigning:      Boolean              = false,
    val reassignReason:     String               = "",     // motivo opcional
    val snackbar:           String?              = null,
    // Historial de reasignación
    val showHistory:        Boolean              = false,
    val loadingHistory:     Boolean              = false,
    val history:            List<RouteAssignmentDto> = emptyList(),
    // Selector de fecha para rutas multi-día
    val availableDates: List<String>              = emptyList(),
    // null hasta que se cargan las fechas reales de la ruta. Antes arrancaba
    // valiendo HOY, y ese valor transitorio se filtraba a quien lo leyera antes
    // de la correccion (la barra de jornada, entre otros).
    val selectedDate:   String?                   = null,
    // Modelo C: visita actual de cada stop en la fecha seleccionada
    // (stopUid → StopVisitEntity). Si no hay visita aún para esa fecha,
    // el stop no aparece en el mapa y la UI muestra "pending" por defecto.
    val visitsByStop:   Map<String, StopVisitEntity> = emptyMap(),
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepo:    RouteRepository,
    private val adminRepo:    AdminRepository,
    private val stopRepo:     StopRepository,
    private val visitRepo:    StopVisitRepository,
    private val locationMgr:  LocationManager,
    private val kpiValueDao:  KpiValueDao,
    private val prefsRepo:    UserPrefsRepository,
    private val session:      SessionManager,
) : BaseViewModel() {

    private val routeUid: String = checkNotNull(savedStateHandle["routeUid"])

    private val _ui = MutableStateFlow(RouteDetailUiState(
        canEditStops = UserRole.from(session.userRole).canEditStops,
        canReassign  = UserRole.from(session.userRole).canReassignRoutes,
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
                routeOwnerName = route?.let { r ->
                    if (r.userId != session.userId) {
                        runCatching { adminRepo.listUsers() }.getOrNull()
                            ?.let { res ->
                                if (res is com.pabl3st.rutapp.data.repository.AuthResult.Success)
                                    res.data.firstOrNull { it.userId == r.userId }?.displayName
                                else null
                            }
                    } else null
                },
            ) }
        }
    }

    private fun observeStops() {
        viewModelScope.launch {
            // Modelo C — informes diarios independientes:
            // Las fechas las da la ruta (route.dateAssigned + route.scheduledDates),
            // NO los stops. Cada PDV es único; lo que cambia por fecha es la visita
            // asociada en stop_visits.
            val route = routeRepo.getByUid(routeUid)
            val routeDates: List<String> = buildList {
                if (route != null) {
                    if (route.dateAssigned.isNotBlank()) add(route.dateAssigned)
                    route.scheduledDates?.forEach { d -> if (d.isNotBlank()) add(d) }
                }
            }.distinct().sorted()

            val availDates: List<String> = if (routeDates.isNotEmpty()) {
                routeDates
            } else {
                // Fallback compat: rutas legacy sin scheduledDates → fechas de stops
                stopRepo.getDistinctDates(routeUid)
            }

            if (availDates.isNotEmpty()) {
                // Seleccionar por defecto la próxima fecha futura (o la primera si todas pasaron)
                val today = java.time.LocalDate.now().toString()
                val nextFuture = availDates.firstOrNull { it >= today } ?: availDates.first()
                val selected   = _ui.value.selectedDate?.takeIf { it in availDates } ?: nextFuture
                _ui.update { it.copy(availableDates = availDates, selectedDate = selected) }
                // Asegurar stop_visits creadas para la fecha seleccionada
                ensureVisitsForDate(routeUid, selected)
            } else {
                _ui.update { it.copy(availableDates = emptyList(), selectedDate = null) }
            }

            // En Modelo C los stops mostrados son TODOS los de la ruta (PDVs únicos).
            // El selector de fecha solo cambia qué stop_visit se asocia a cada uno.
            stopRepo.observeByRoute(routeUid)
                .catch { e -> _ui.update { it.copy(error = e.message) } }
                .collect { stops ->
                    _baseStops.value = stops
                    applySortMode(_ui.value.sortMode, stops)
                }
        }

        // Flow reactivo: cada vez que cambia selectedDate, recargar el mapa
        // visitsByStop con las visitas reales de esa fecha. Si una visita se
        // marca "done", el StopCard correspondiente cambia sin recargar todo.
        viewModelScope.launch {
            _ui.map { it.selectedDate }
                .distinctUntilChanged()
                .flatMapLatest { date ->
                    if (date.isNullOrBlank()) flowOf(emptyList())
                    else visitRepo.observeByRouteAndDate(routeUid, date)
                }
                .collect { visits ->
                    _ui.update { it.copy(visitsByStop = visits.associateBy { v -> v.stopUid }) }
                }
        }
    }

    /**
     * Crea una stop_visit "pending" por cada parada de la ruta para la fecha
     * dada. Idempotente — si ya existe, no la duplica.
     */
    private suspend fun ensureVisitsForDate(routeUid: String, date: String) {
        val stops = stopRepo.getByRoute(routeUid)
        stops.forEach { stop ->
            visitRepo.ensureVisitExists(stop.uid, routeUid, date)
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
                            "god"     -> UserRole.from(u.role).level >= UserRole.AGENT.level
                            "owner"   -> UserRole.from(u.role) in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.AGENT)
                            "admin"   -> UserRole.from(u.role) in listOf(UserRole.MANAGER, UserRole.AGENT)
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
            routeRepo.reassignRoute(routeUid, targetId, _ui.value.reassignReason)
                .onSuccess {
                    val name = _ui.value.assignableUsers
                        .firstOrNull { it.userId == targetId }?.displayName ?: "usuario"
                    _ui.update { it.copy(
                        isReassigning      = false,
                        showReassignDialog = false,
                        selectedAssigneeId = null,
                        reassignReason     = "",
                        snackbar           = "Ruta reasignada a $name",
                    ) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isReassigning = false, error = e.message ?: "Error al reasignar") }
                }
        }
    }

    fun onReassignReasonChange(v: String) = _ui.update { it.copy(reassignReason = v) }

    // ── Historial de reasignación ─────────────────────────────
    fun onShowHistory() {
        _ui.update { it.copy(showHistory = true, loadingHistory = true) }
        viewModelScope.launch {
            routeRepo.fetchRouteHistory(routeUid)
                .onSuccess { h -> _ui.update { it.copy(history = h, loadingHistory = false) } }
                .onFailure { e ->
                    _ui.update { it.copy(loadingHistory = false, error = e.message) }
                }
        }
    }

    fun onDismissHistory() = _ui.update { it.copy(showHistory = false) }

    fun removeStop(stopUid: String) {
        viewModelScope.launch { stopRepo.removeFromRoute(stopUid) }
    }

    fun clearRouteStops() {
        val routeUid = _ui.value.route?.uid ?: return
        viewModelScope.launch {
            stopRepo.clearRoute(routeUid)
            _ui.update { it.copy(snackbar = "Ruta vaciada") }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }

    fun onDateSelected(date: String) {
        _ui.update { it.copy(selectedDate = date) }
        viewModelScope.launch {
            // En Modelo C la lista de stops no depende de la fecha: son siempre
            // los mismos PDVs. Lo que cambia es el mapa visitsByStop, que se
            // recarga reactivamente vía el flow lanzado en observeStops().
            // Lo único que falta hacer aquí es asegurar que existan stop_visits
            // pendientes para la nueva fecha.
            ensureVisitsForDate(routeUid, date)
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



package com.pabl3st.rutapp.feature.biblioteca

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.StopTagConfig
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.AdminRepository
import com.pabl3st.rutapp.data.repository.AuthResult
import com.pabl3st.rutapp.data.network.AccountUserDto
import com.pabl3st.rutapp.core.UserRole
import com.pabl3st.rutapp.data.repository.UserPrefsRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BibliotecaTab(val label: String, val icon: ImageVector) {
    ALL("Todas", Icons.Default.GridView),
    NO_GPS("Sin GPS", Icons.Default.GpsOff),
    ORPHAN("Sin ruta", Icons.Default.LinkOff),
}

data class BibliotecaUiState(
    val tab: BibliotecaTab              = BibliotecaTab.ALL,
    val query: String                   = "",
    val filteredStops: List<StopEntity> = emptyList(),
    val isLoading: Boolean              = true,
    // Solo tags con condición ALWAYS o de estado estático (no de jornada)
    val stopTags: List<StopTagConfig>   = emptyList(),
    // ── Asignar paradas sueltas a un comercial (manager+) ──
    val canAssign: Boolean              = false,
    val selecting: Boolean              = false,
    val selected: Set<String>           = emptySet(),
    val showAssign: Boolean             = false,
    val assignableUsers: List<AccountUserDto> = emptyList(),
    val selectedAgentId: Int?           = null,
    val dayOffset: Int                  = 0,        // 0 hoy, 1 mañana, 2 pasado
    val routeName: String               = "",
    val isAssigning: Boolean            = false,
    val snackbar: String?               = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class BibliotecaViewModel @Inject constructor(
    private val stopRepo:  StopRepository,
    private val routeRepo: RouteRepository,
    private val adminRepo: AdminRepository,
    private val prefsRepo: UserPrefsRepository,
    private val session:   SessionManager,
) : BaseViewModel() {

    private val _tab   = MutableStateFlow(BibliotecaTab.ALL)
    private val _query = MutableStateFlow("")

    private val _ui = MutableStateFlow(BibliotecaUiState())
    val ui: StateFlow<BibliotecaUiState> = _ui.asStateFlow()

    init {
        _ui.update { it.copy(canAssign = UserRole.from(session.userRole).canReassignRoutes) }
        observeTags()
        viewModelScope.launch {
            combine(_tab, _query.debounce(200)) { tab, query -> tab to query }
                .flatMapLatest { (tab, query) ->
                    val base = when (tab) {
                        BibliotecaTab.ALL    -> stopRepo.observeAll(session.accountId)
                        BibliotecaTab.NO_GPS -> stopRepo.observeWithoutGps(session.accountId)
                        BibliotecaTab.ORPHAN -> stopRepo.observeOrphaned(session.accountId)
                    }
                    base.map { stops ->
                        // Deduplicar por externalId: mostrar un solo PDV por externalId
                        // (el más reciente por visitedAt, o el que tenga dateAssigned futuro)
                        val deduped = stops
                            .groupBy { it.externalId ?: it.uid }
                            .mapValues { (_, group) ->
                                // Preferir el de hoy si existe, si no el más reciente visitado
                                val today = java.time.LocalDate.now().toString()
                                group.firstOrNull { it.dateAssigned == today }
                                    ?: group.maxByOrNull { it.visitedAt ?: it.dateAssigned ?: "" }
                                    ?: group.first()
                            }
                            .values.toList()
                            .sortedWith(compareByDescending { it.visitedAt ?: it.updatedAt })

                        val filtered = if (query.isBlank()) deduped
                        else deduped.filter {
                            it.name.contains(query, ignoreCase = true) ||
                            it.address?.contains(query, ignoreCase = true) == true ||
                            it.externalId?.contains(query, ignoreCase = true) == true
                        }
                        Triple(tab, query, filtered)
                    }
                }
                .collect { (tab, query, filtered) ->
                    _ui.update { it.copy(tab = tab, query = query, filteredStops = filtered, isLoading = false) }
                }
        }
    }

    private fun observeTags() {
        viewModelScope.launch {
            prefsRepo.prefs.collect { prefs ->
                // Biblioteca solo muestra tags con condición ALWAYS, accountStatus implícito,
                // o de estado estático — no tags de jornada (PDV_OPEN/CLOSED, RESULT_IS, KPI_*)
                val staticConditions = setOf(
                    com.pabl3st.rutapp.data.local.entity.TagCondition.ALWAYS,
                    com.pabl3st.rutapp.data.local.entity.TagCondition.STATUS_DONE,
                    com.pabl3st.rutapp.data.local.entity.TagCondition.STATUS_PENDING,
                    com.pabl3st.rutapp.data.local.entity.TagCondition.DAYS_SINCE_VISIT,
                )
                _ui.update { it.copy(stopTags = prefs.stopTags.filter { t -> t.condition in staticConditions }) }
            }
        }
    }

    fun onTabChange(tab: BibliotecaTab) { _tab.value = tab }
    fun onQueryChange(q: String)        { _query.value = q }

    // ── Selección y asignación de paradas sueltas ─────────────
    fun toggleSelecting() = _ui.update {
        if (it.selecting) it.copy(selecting = false, selected = emptySet())
        else it.copy(selecting = true)
    }
    fun toggleSelect(uid: String) = _ui.update {
        val next = if (uid in it.selected) it.selected - uid else it.selected + uid
        it.copy(selected = next, selecting = true)
    }
    fun selectAllVisible() = _ui.update { it.copy(selected = it.filteredStops.map { s -> s.uid }.toSet()) }

    fun openAssign() {
        if (_ui.value.selected.isEmpty()) return
        _ui.update { it.copy(showAssign = true, dayOffset = 0, selectedAgentId = null,
            routeName = "Ruta " + java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("es")))) }
        viewModelScope.launch {
            when (val r = adminRepo.listUsers()) {
                is AuthResult.Success -> {
                    val role = session.userRole
                    val users = r.data.filter { u ->
                        u.isActive && when (role) {
                            "god"     -> UserRole.from(u.role).level >= UserRole.AGENT.level
                            "owner"   -> UserRole.from(u.role) in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.AGENT)
                            "admin"   -> UserRole.from(u.role) in listOf(UserRole.MANAGER, UserRole.AGENT)
                            "manager" -> true
                            else      -> false
                        }
                    }
                    _ui.update { it.copy(assignableUsers = users) }
                }
                is AuthResult.Error -> _ui.update { it.copy(snackbar = "No se pudo cargar el equipo") }
            }
        }
    }
    fun dismissAssign()              = _ui.update { it.copy(showAssign = false) }
    fun pickAgent(id: Int)           = _ui.update { it.copy(selectedAgentId = id) }
    fun pickDay(offset: Int)         = _ui.update { it.copy(dayOffset = offset) }
    fun onRouteName(v: String)       = _ui.update { it.copy(routeName = v) }
    fun clearSnackbar()              = _ui.update { it.copy(snackbar = null) }

    /** Crea UNA ruta para el comercial con las paradas elegidas (todas pasan a el). */
    fun confirmAssign() {
        val st = _ui.value
        val agentId = st.selectedAgentId ?: return
        if (st.isAssigning || st.selected.isEmpty() || st.routeName.isBlank()) return
        val date = java.time.LocalDate.now().plusDays(st.dayOffset.toLong()).toString()
        viewModelScope.launch {
            _ui.update { it.copy(isAssigning = true) }
            runCatching {
                val route = routeRepo.createRoute(
                    name = st.routeName.trim(), dateAssigned = date, forUserId = agentId,
                )
                st.selected.forEach { stopRepo.addToRoute(it, route.uid, date) }
            }.onSuccess {
                val who = st.assignableUsers.firstOrNull { it.userId == agentId }?.displayName ?: "el comercial"
                _ui.update { it.copy(isAssigning = false, showAssign = false, selecting = false,
                    selected = emptySet(),
                    snackbar = "${st.selected.size} paradas asignadas a $who") }
            }.onFailure { e ->
                _ui.update { it.copy(isAssigning = false, snackbar = e.message ?: "Error al asignar") }
            }
        }
    }
}

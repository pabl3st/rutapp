package com.pabl3st.rutapp.feature.admin

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.network.AccountUserDto
import com.pabl3st.rutapp.data.network.InviteDto
import com.pabl3st.rutapp.data.repository.AdminRepository
import com.pabl3st.rutapp.data.repository.AuthResult
import com.pabl3st.rutapp.data.network.StatsMonthAgent
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.repository.SyncRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val userName:    String  = "",
    val userEmail:   String  = "",
    val userRole:    String  = "",
    val accountName: String  = "",
    val accountType: String  = "",
    val totalRoutes: Int     = 0,
    val totalStops:  Int     = 0,
    val pendingSync: Int     = 0,
    val users:           List<AccountUserDto> = emptyList(),
    val usersLoading:    Boolean = false,
    val canManageUsers:  Boolean = false,
    val showInviteDialog:  Boolean = false,
    val inviteEmail:       String  = "",
    val inviteRole:        String  = "agent",
    val isSendingInvite:   Boolean = false,
    // Código generado — se muestra en un diálogo al admin para que lo comparta
    val generatedCode:     String? = null,
    val invites:           List<InviteDto> = emptyList(),
    val invitesLoading:    Boolean = false,
    val showRolePicker:    Boolean = false,
    val rolePickerUser:    AccountUserDto? = null,
    val isChangingRole:    Boolean = false,
    val showManagerPicker:  Boolean          = false,
    val managerPickerUser:  AccountUserDto?  = null,
    val isAssigningManager: Boolean          = false,
    val isLoading:    Boolean = true,
    val error:        String? = null,
    val snackbar:     String? = null,
    // Panel "Mis reportadores" — para manager y admin
    val directReports:        List<AccountUserDto> = emptyList(),
    val directReportsLoading: Boolean              = false,
    val showDirectReports:    Boolean              = false,  // visible para manager/admin
    // Rutas activas hoy por reportador (userId -> count)
    val reporterRouteCounts:  Map<Int, Int>        = emptyMap(),
    val reporterDoneStops:    Map<Int, Int>        = emptyMap(),
    val reporterPendingStops: Map<Int, Int>        = emptyMap(),
    // KPIs del equipo desde servidor (stats_month)
    val reporterServerStats:  List<StatsMonthAgent> = emptyList(),
    val isLoadingKpis:        Boolean               = false,
    // Borrar todas las rutas
    val showClearDialog:      Boolean               = false,
    val isClearingRoutes:     Boolean               = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel @Inject constructor(
    private val session:   SessionManager,
    private val api:       RutasApiService,
    private val syncRepo:  SyncRepository,
    private val routeRepo: RouteRepository,
    private val stopRepo:  StopRepository,
    private val adminRepo: AdminRepository,
) : BaseViewModel() {

    private val _ui = MutableStateFlow(
        AdminUiState(
            userName      = session.userDisplayName.ifBlank { session.userName },
            userEmail     = session.userEmail,
            userRole      = session.userRole,
            accountName   = session.accountName,
            accountType   = session.accountType,
            canManageUsers = adminRepo.canManageUsers && !adminRepo.isGod, // god tiene su propio dashboard
        )
    )
    val ui: StateFlow<AdminUiState> = _ui.asStateFlow()

    init {
        loadStats()
        if (adminRepo.canManageUsers && !adminRepo.isGod) {
            loadUsers()
            loadInvites()
        }
        // Manager y admin ven su panel de reportadores
        val role = session.userRole
        if (role in setOf("manager", "admin", "owner")) {
            _ui.update { it.copy(showDirectReports = true) }
            loadDirectReports()
            loadReporterKpis()
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val pending = syncRepo.pendingCount()
            routeRepo.observeAll()
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .flatMapLatest { routes ->
                    val uids = routes.map { it.uid }
                    stopRepo.observeByRouteUids(uids).map { stops -> routes.size to stops.size }
                }
                .collect { (routeCount, stopCount) ->
                    _ui.update { it.copy(
                        totalRoutes = routeCount,
                        totalStops  = stopCount,
                        pendingSync = pending,
                        isLoading   = false,
                    )}
                }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _ui.update { it.copy(usersLoading = true) }
            when (val result = adminRepo.listUsers()) {
                is AuthResult.Success -> _ui.update { it.copy(users = result.data, usersLoading = false) }
                is AuthResult.Error   -> _ui.update { it.copy(error = result.message, usersLoading = false) }
            }
        }
    }

    fun loadDirectReports() {
        viewModelScope.launch {
            _ui.update { it.copy(directReportsLoading = true) }
            when (val result = adminRepo.listUsers()) {
                is AuthResult.Success -> {
                    val myId   = session.userId
                    val myRole = session.userRole
                    // Filtrar según rol:
                    // manager → sus agentes directos (managerId == myId)
                    // admin   → managers + agents de su account
                    // owner   → todos (admin/manager/agent)
                    // Managers directos de este admin (managerId == myId)
                    val myDirectManagerIds = result.data
                        .filter { it.managerId == myId && it.role == "manager" && it.isActive }
                        .map { it.userId }
                        .toSet()

                    val reports = result.data.filter { u ->
                        u.isActive && when (myRole) {
                            // manager: ya recibe solo sus reportadores del servidor (users_list filtrado)
                            "manager" -> true
                            // admin: sus managers directos + agentes de esos managers
                            "admin"   -> u.managerId == myId ||
                                         (u.role == "agent" && u.managerId != null && u.managerId in myDirectManagerIds)
                            // owner: todos (admin/manager/agent)
                            "owner"   -> u.role in setOf("admin", "manager", "agent")
                            else      -> false
                        }
                    }
                    _ui.update { it.copy(directReports = reports, directReportsLoading = false) }
                    // Cargar stats de rutas de hoy para cada reportador
                    loadReporterStats(reports.map { it.userId })
                }
                is AuthResult.Error -> _ui.update { it.copy(directReportsLoading = false) }
            }
        }
    }

    private fun loadReporterStats(userIds: List<Int>) {
        if (userIds.isEmpty()) return
        viewModelScope.launch {
            val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            routeRepo.observeAll()
                .flatMapLatest { routes ->
                    // Filtrar rutas de hoy de los reportadores
                    val todayRoutes = routes.filter { r ->
                        r.userId in userIds &&
                        (r.dateAssigned == today || r.scheduledDates?.contains(today) == true)
                    }
                    val routeUids = todayRoutes.map { it.uid }
                    stopRepo.observeByRouteUids(routeUids).map { stops ->
                        // routeCounts: cuántas rutas tiene cada userId hoy
                        val routeCounts = todayRoutes.groupBy { it.userId }.mapValues { it.value.size }
                        // done / pending stops por userId
                        val stopsByRoute = stops.groupBy { it.routeUid }
                        val doneByUser   = mutableMapOf<Int, Int>()
                        val pendByUser   = mutableMapOf<Int, Int>()
                        todayRoutes.forEach { route ->
                            val routeStops = stopsByRoute[route.uid] ?: emptyList()
                            val done = routeStops.count { it.status == "done" }
                            val pend = routeStops.count { it.status != "done" }
                            doneByUser[route.userId]  = (doneByUser[route.userId]  ?: 0) + done
                            pendByUser[route.userId]  = (pendByUser[route.userId]  ?: 0) + pend
                        }
                        Triple(routeCounts, doneByUser.toMap(), pendByUser.toMap())
                    }
                }
                .catch { /* silencioso — stats no críticos */ }
                .collect { (counts, done, pend) ->
                    _ui.update { it.copy(
                        reporterRouteCounts  = counts,
                        reporterDoneStops    = done,
                        reporterPendingStops = pend,
                    )}
                }
        }
    }

    fun loadReporterKpis() {
        val token = session.token ?: return
        val myRole = session.userRole
        if (myRole !in setOf("manager", "admin", "owner", "god")) return
        viewModelScope.launch {
            _ui.update { it.copy(isLoadingKpis = true) }
            runCatching {
                val month = java.time.YearMonth.now().toString()
                val resp  = api.statsMonth(token = token, month = month)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    val agents = resp.body()!!.agents
                    _ui.update { it.copy(reporterServerStats = agents, isLoadingKpis = false) }
                } else {
                    _ui.update { it.copy(isLoadingKpis = false) }
                }
            }.onFailure {
                _ui.update { it.copy(isLoadingKpis = false) }
            }
        }
    }

    fun onShowInviteDialog()           = _ui.update { it.copy(showInviteDialog = true, inviteEmail = "", inviteRole = "agent") }
    fun onDismissInviteDialog()        = _ui.update { it.copy(showInviteDialog = false) }
    fun onInviteEmailChange(v: String) = _ui.update { it.copy(inviteEmail = v) }
    fun onInviteRoleChange(v: String)  = _ui.update { it.copy(inviteRole = v) }

    fun sendInvite() {
        val s = _ui.value
        if (s.inviteEmail.isBlank()) {
            _ui.update { it.copy(error = "El email es obligatorio") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isSendingInvite = true) }
            when (val result = adminRepo.inviteUser(s.inviteEmail.trim(), s.inviteRole)) {
                is AuthResult.Success -> {
                    // result.data es el código de invitación generado
                    _ui.update { it.copy(
                        showInviteDialog = false,
                        isSendingInvite  = false,
                        generatedCode    = result.data,
                    ) }
                    loadUsers()
                    loadInvites()
                }
                is AuthResult.Error -> _ui.update { it.copy(
                    isSendingInvite = false,
                    error = result.message,
                ) }
            }
        }
    }

    fun onDismissGeneratedCode() = _ui.update { it.copy(generatedCode = null) }

    fun loadInvites() {
        viewModelScope.launch {
            _ui.update { it.copy(invitesLoading = true) }
            when (val result = adminRepo.listInvites()) {
                is AuthResult.Success -> _ui.update { it.copy(invites = result.data, invitesLoading = false) }
                is AuthResult.Error   -> _ui.update { it.copy(invitesLoading = false) }
            }
        }
    }

    fun deleteInvite(inviteId: Int) {
        viewModelScope.launch {
            when (val result = adminRepo.deleteInvite(inviteId)) {
                is AuthResult.Success -> { loadInvites(); _ui.update { it.copy(snackbar = "Invitación eliminada") } }
                is AuthResult.Error   -> _ui.update { it.copy(error = result.message) }
            }
        }
    }

    fun onShowRolePicker(user: AccountUserDto) = _ui.update { it.copy(showRolePicker = true, rolePickerUser = user) }
    fun onDismissRolePicker()                  = _ui.update { it.copy(showRolePicker = false, rolePickerUser = null) }

    fun onSelectRole(role: String) {
        val user = _ui.value.rolePickerUser ?: return
        viewModelScope.launch {
            _ui.update { it.copy(showRolePicker = false, rolePickerUser = null, isChangingRole = true) }
            when (val result = adminRepo.updateRole(user.userId, role)) {
                is AuthResult.Success -> {
                    _ui.update { it.copy(isChangingRole = false, snackbar = "Rol de ${user.displayName} actualizado") }
                    loadUsers()
                }
                is AuthResult.Error -> _ui.update { it.copy(isChangingRole = false, error = result.message) }
            }
        }
    }

    fun deactivateUser(user: AccountUserDto) {
        viewModelScope.launch {
            when (val result = adminRepo.deactivateUser(user.userId)) {
                is AuthResult.Success -> { _ui.update { it.copy(snackbar = "${user.displayName} desactivado") }; loadUsers() }
                is AuthResult.Error   -> _ui.update { it.copy(error = result.message) }
            }
        }
    }

    fun reactivateUser(user: AccountUserDto) {
        viewModelScope.launch {
            when (val result = adminRepo.reactivateUser(user.userId)) {
                is AuthResult.Success -> { _ui.update { it.copy(snackbar = "${user.displayName} reactivado") }; loadUsers() }
                is AuthResult.Error   -> _ui.update { it.copy(error = result.message) }
            }
        }
    }

    fun roleLabel(role: String) = adminRepo.roleLabel(role)
    val availableRoles get()    = adminRepo.availableRoles

    // ── Asignación de supervisor ──────────────────────────────
    fun onShowManagerPicker(user: AccountUserDto) =
        _ui.update { it.copy(showManagerPicker = true, managerPickerUser = user) }

    fun onDismissManagerPicker() =
        _ui.update { it.copy(showManagerPicker = false, managerPickerUser = null) }

    fun onAssignManager(managerId: Int?) {
        val target = _ui.value.managerPickerUser ?: return
        viewModelScope.launch {
            _ui.update { it.copy(
                showManagerPicker  = false,
                managerPickerUser  = null,
                isAssigningManager = true,
            ) }
            when (val result = adminRepo.assignManager(target.userId, managerId)) {
                is AuthResult.Success -> {
                    val msg = if (managerId != null) "Supervisor asignado a ${target.displayName}"
                              else "Supervisor eliminado de ${target.displayName}"
                    _ui.update { it.copy(isAssigningManager = false, snackbar = msg) }
                    loadUsers()
                }
                is AuthResult.Error -> _ui.update { it.copy(
                    isAssigningManager = false, error = result.message,
                ) }
            }
        }
    }

    fun validManagersFor(targetRole: String): List<AccountUserDto> =
        adminRepo.validManagersFor(_ui.value.users, targetRole)

    fun clearError()    = _ui.update { it.copy(error = null) }
    fun clearSnackbar() = _ui.update { it.copy(snackbar = null) }

    fun onClearRoutesRequest() = _ui.update { it.copy(showClearDialog = true) }
    fun onClearRoutesDismiss() = _ui.update { it.copy(showClearDialog = false) }

    fun confirmClearRoutes() {
        _ui.update { it.copy(showClearDialog = false, isClearingRoutes = true) }
        viewModelScope.launch {
            val ok = routeRepo.clearAllRoutes()
            _ui.update {
                it.copy(
                    isClearingRoutes = false,
                    snackbar = if (ok) "Todas las rutas y paradas eliminadas"
                               else "Error al eliminar — comprueba la conexión",
                )
            }
        }
    }
    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(
            isLoading = false,
            error     = t.message ?: "Error inesperado",
        )}
    }

}


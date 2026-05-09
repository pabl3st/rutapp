package com.pabl3st.rutapp.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.network.GodAccountDto
import com.pabl3st.rutapp.data.network.GodUserDto
import com.pabl3st.rutapp.data.repository.AdminRepository
import com.pabl3st.rutapp.data.repository.AuthResult
import com.pabl3st.rutapp.data.repository.roleLevelOf
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GodDashboardUiState(
    // Stats globales
    val totalAccounts: Int = 0,
    val totalUsers:    Int = 0,
    val totalRoutes:   Int = 0,
    val totalStops:    Int = 0,
    val totalReports:  Int = 0,

    // Tablas
    val topAccounts:  List<GodAccountDto> = emptyList(),
    val recentUsers:  List<GodUserDto>    = emptyList(),
    val allUsers:     List<GodUserDto>    = emptyList(),

    // Filtros de búsqueda de usuarios
    val userSearch:   String  = "",
    val roleFilter:   String  = "",   // "" = todos

    // Tab activo
    val activeTab:    GodTab  = GodTab.OVERVIEW,

    // Dialogs
    val rolePickerUser:  GodUserDto? = null,
    val showRolePicker:  Boolean     = false,
    val pendingNewRole:  String      = "",

    // Estado
    val isLoadingStats: Boolean = true,
    val isLoadingUsers: Boolean = false,
    val error:          String? = null,
    val snackbar:       String? = null,
)

enum class GodTab(val label: String) {
    OVERVIEW("Resumen"),
    ACCOUNTS("Cuentas"),
    USERS("Usuarios"),
}

@HiltViewModel
class GodDashboardViewModel @Inject constructor(
    private val session:   SessionManager,
    private val adminRepo: AdminRepository,
    private val api:       com.pabl3st.rutapp.data.network.RutasApiService,
) : ViewModel() {

    private val _ui = MutableStateFlow(GodDashboardUiState())
    val ui: StateFlow<GodDashboardUiState> = _ui.asStateFlow()

    val roleOptions = listOf("", "god", "owner", "admin", "manager", "agent", "viewer")

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoadingStats = true) }
            runCatching {
                val resp = api.godStats(token = "Bearer ${session.token}")
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.success == true) {
                        _ui.update { it.copy(
                            totalAccounts = body.totalAccounts,
                            totalUsers    = body.totalUsers,
                            totalRoutes   = body.totalRoutes,
                            totalStops    = body.totalStops,
                            totalReports  = body.totalReports,
                            topAccounts   = body.topAccounts,
                            recentUsers   = body.recentUsers,
                            isLoadingStats = false,
                        )}
                    } else {
                        _ui.update { it.copy(error = body?.message ?: "Error", isLoadingStats = false) }
                    }
                } else {
                    _ui.update { it.copy(error = "HTTP ${resp.code()}", isLoadingStats = false) }
                }
            }.onFailure { e ->
                _ui.update { it.copy(error = e.message ?: "Error de red", isLoadingStats = false) }
            }
        }
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoadingUsers = true) }
            val s = _ui.value
            val body = buildMap<String, Any?> {
                if (s.userSearch.isNotBlank()) put("search", s.userSearch)
                if (s.roleFilter.isNotBlank()) put("role", s.roleFilter)
            }
            runCatching {
                val resp = api.godUsersAll(token = "Bearer ${session.token}", body = body)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    _ui.update { it.copy(allUsers = resp.body()!!.users, isLoadingUsers = false) }
                } else {
                    _ui.update { it.copy(error = "Error al cargar usuarios", isLoadingUsers = false) }
                }
            }.onFailure { e ->
                _ui.update { it.copy(error = e.message, isLoadingUsers = false) }
            }
        }
    }

    fun onTabChange(tab: GodTab) {
        _ui.update { it.copy(activeTab = tab) }
        if (tab == GodTab.USERS && _ui.value.allUsers.isEmpty()) loadAllUsers()
    }

    fun onUserSearchChange(q: String) {
        _ui.update { it.copy(userSearch = q) }
    }

    fun onRoleFilterChange(role: String) {
        _ui.update { it.copy(roleFilter = role) }
        loadAllUsers()
    }

    fun applyUserSearch() = loadAllUsers()

    // ── Cambio de rol ─────────────────────────────────────────
    fun onShowRolePicker(user: GodUserDto) {
        _ui.update { it.copy(rolePickerUser = user, pendingNewRole = user.role, showRolePicker = true) }
    }
    fun onDismissRolePicker() = _ui.update { it.copy(showRolePicker = false, rolePickerUser = null) }
    fun onPendingRoleChange(role: String) = _ui.update { it.copy(pendingNewRole = role) }

    fun confirmRoleChange() {
        val user = _ui.value.rolePickerUser ?: return
        val newRole = _ui.value.pendingNewRole
        if (newRole == user.role) { onDismissRolePicker(); return }
        viewModelScope.launch {
            _ui.update { it.copy(showRolePicker = false, rolePickerUser = null) }
            when (val r = adminRepo.godSetRole(user.id, newRole)) {
                is AuthResult.Success -> {
                    _ui.update { it.copy(snackbar = "${user.displayName} → ${adminRepo.roleLabel(newRole)}") }
                    loadAllUsers()
                }
                is AuthResult.Error -> _ui.update { it.copy(error = r.message) }
            }
        }
    }

    fun roleLabel(role: String) = adminRepo.roleLabel(role)

    // ── Nivel de autoridad del usuario actual (para limitar qué puede cambiar) ──
    fun canEditUser(targetRole: String): Boolean =
        roleLevelOf("god") > roleLevelOf(targetRole) || targetRole != "god"

    fun clearError()    = _ui.update { it.copy(error = null) }
    fun clearSnackbar() = _ui.update { it.copy(snackbar = null) }
}

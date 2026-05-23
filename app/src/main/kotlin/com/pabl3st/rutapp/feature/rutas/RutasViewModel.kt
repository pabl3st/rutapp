package com.pabl3st.rutapp.feature.rutas

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.AdminRepository
import com.pabl3st.rutapp.data.network.AccountUserDto
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class RutasUiState(
    val routes: List<RouteEntity> = emptyList(),
    val isLoading: Boolean        = true,
    val isSyncing: Boolean        = false,
    val showCreateDialog: Boolean = false,
    val newRouteName: String      = "",
    val newRouteDate: String      = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val userRole: String          = "agent",
    val currentUserId: Int        = 0,
    val canCreate: Boolean        = false,
    val canDelete: Boolean        = false,
    val error: String?            = null,
    // Asignación a usuario — solo para owner/admin/manager al crear
    val assignableUsers:    List<AccountUserDto> = emptyList(),
    val selectedAssigneeId: Int?                 = null,
    val loadingUsers:       Boolean              = false,
    // Map userId → displayName para mostrar etiqueta en cards de ruta
    val teamMembers:        Map<Int, String>     = emptyMap(),
)

@HiltViewModel
class RutasViewModel @Inject constructor(
    private val routeRepo:  RouteRepository,
    private val adminRepo:  AdminRepository,
    private val session:    SessionManager,
) : BaseViewModel() {

    private val _ui = MutableStateFlow(RutasUiState(
        userRole      = session.userRole,
        currentUserId = session.userId,
        canCreate = session.userRole in listOf("owner", "admin", "manager", "god"),
        canDelete = session.userRole in listOf("owner", "admin", "god"),
    ))
    val ui: StateFlow<RutasUiState> = _ui.asStateFlow()

    init {
        observeRoutes()
    }

    private fun observeRoutes() {
        viewModelScope.launch {
            routeRepo.observeAll()
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { routes -> _ui.update { it.copy(routes = routes, isLoading = false) } }
        }
    }

    // ── Crear ruta ────────────────────────────────────────────
    fun onShowCreateDialog() {
        _ui.update { it.copy(showCreateDialog = true, selectedAssigneeId = null, assignableUsers = emptyList()) }
        loadAssignableUsers()
    }
    fun onDismissCreateDialog()         = _ui.update { it.copy(showCreateDialog = false, newRouteName = "", error = null, selectedAssigneeId = null, assignableUsers = emptyList()) }
    fun onNewRouteNameChange(v: String) = _ui.update { it.copy(newRouteName = v) }
    fun onNewRouteDateChange(v: String) = _ui.update { it.copy(newRouteDate = v) }

    fun onSelectAssignee(userId: Int?) = _ui.update { it.copy(selectedAssigneeId = userId) }

    private fun loadAssignableUsers() {
        val role = session.userRole
        // solo roles que pueden asignar a inferiores necesitan la lista
        if (role !in listOf("owner", "admin", "manager", "god")) return
        viewModelScope.launch {
            _ui.update { it.copy(loadingUsers = true) }
            when (val r = adminRepo.listUsers()) {
                is com.pabl3st.rutapp.data.repository.AuthResult.Success -> {
                    val myRole = session.userRole
                    val myId   = session.userId
                    // Filtrar: solo usuarios activos de rol inferior que reportan a mí o a mi equipo
                    val assignable = r.data.filter { u ->
                        u.isActive && u.userId != myId && when (myRole) {
                            "god"   -> u.role in listOf("owner", "admin", "manager", "agent")
                            "owner" -> u.role in listOf("admin", "manager", "agent")
                            "admin" -> u.role in listOf("manager", "agent")
                            "manager" -> true  // servidor ya filtró solo los reportadores directos
                            else   -> false
                        }
                    }
                    val teamMap = r.data.associate { it.userId to it.displayName }
                    _ui.update { it.copy(assignableUsers = assignable, loadingUsers = false, teamMembers = teamMap) }
                }
                is com.pabl3st.rutapp.data.repository.AuthResult.Error ->
                    _ui.update { it.copy(loadingUsers = false) }
            }
        }
    }

    fun createRoute() {
        val name = _ui.value.newRouteName.trim()
        if (name.isBlank()) {
            _ui.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        // Fecha por defecto = hoy. Se asigna al calendario manualmente después.
        val date = _ui.value.newRouteDate.ifBlank {
            java.time.LocalDate.now().toString()
        }
        viewModelScope.launch {
            routeRepo.createRoute(
                name         = name,
                dateAssigned = date,
                forUserId    = _ui.value.selectedAssigneeId,
            )
            _ui.update { it.copy(showCreateDialog = false, newRouteName = "", newRouteDate = "", selectedAssigneeId = null) }
        }
    }

    fun syncNow() {
        if (_ui.value.isSyncing) return
        viewModelScope.launch {
            _ui.update { it.copy(isSyncing = true) }
            routeRepo.fetchDelta()
            _ui.update { it.copy(isSyncing = false) }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(
            isLoading = false,
            error     = t.message ?: "Error inesperado",
        )}
    }

}

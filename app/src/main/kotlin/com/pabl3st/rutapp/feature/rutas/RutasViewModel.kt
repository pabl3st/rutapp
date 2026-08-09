package com.pabl3st.rutapp.feature.rutas

import com.pabl3st.rutapp.core.UserRole

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.AdminRepository
import com.pabl3st.rutapp.data.repository.SyncRepository
import com.pabl3st.rutapp.data.repository.SyncResult
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
    // Conteo de paradas por ruta (routeUid → total/done) para la barra de progreso
    val stopCounts:         Map<String, com.pabl3st.rutapp.data.local.dao.RouteStopCount> = emptyMap(),
    // Modo selección múltiple — reasignación masiva (manager+)
    val selectionMode:      Boolean              = false,
    val selectedRouteUids:  Set<String>          = emptySet(),
    val showBulkDialog:     Boolean              = false,
    val bulkAssigneeId:     Int?                 = null,
    val bulkReason:         String               = "",
    val isBulkAssigning:    Boolean              = false,
    // Forzar sync manual (owner/admin) — diagnóstico de subida al servidor
    val pendingOpsCount:    Int                  = 0,    // qué hay en la cola en este momento
    val isForceSyncing:     Boolean              = false,
    val forceSyncResult:    String?              = null, // mensaje que se muestra tras pulsar
)

@HiltViewModel
class RutasViewModel @Inject constructor(
    private val routeRepo:    RouteRepository,
    private val adminRepo:    AdminRepository,
    private val session:      SessionManager,
    private val syncRepo:     SyncRepository,
    private val syncQueueDao: SyncQueueDao,
) : BaseViewModel() {

    private val _ui = MutableStateFlow(RutasUiState(
        userRole      = session.userRole,
        currentUserId = session.userId,
        canCreate = UserRole.from(session.userRole).canCreateRoutes,
        canDelete = UserRole.from(session.userRole).canDeleteRoutes,
    ))
    val ui: StateFlow<RutasUiState> = _ui.asStateFlow()

    init {
        observeRoutes()
        observePendingCount()
    }

    private fun observeRoutes() {
        viewModelScope.launch {
            combine(
                routeRepo.observeAll(),
                routeRepo.observeStopCounts(),
            ) { routes, counts -> routes to counts }
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { (routes, counts) ->
                    _ui.update { it.copy(routes = routes, stopCounts = counts, isLoading = false) }
                }
        }
    }

    /** Re-cuenta la cola de sync cada 2 segundos. Barato y útil para mostrar
     *  al owner cuántas operaciones quedan pendientes de subir al servidor. */
    private fun observePendingCount() {
        viewModelScope.launch {
            while (true) {
                val n = runCatching { syncQueueDao.count() }.getOrDefault(0)
                _ui.update { it.copy(pendingOpsCount = n) }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    /** Fuerza el sync de la cola al servidor — botón explícito para owner/admin.
     *  Útil tras importar para confirmar que el batch_sync llega: sustituye al
     *  WorkManager automático que en pruebas no se estaba disparando. */
    fun forceSync() {
        _ui.update { it.copy(isForceSyncing = true, forceSyncResult = null) }
        viewModelScope.launch {
            // El boton de sync forzado es una accion explicita del usuario:
            // reactivamos los ops agotados para que un fallo ya corregido en el
            // servidor no deje operaciones muertas contando en la cola.
            runCatching { syncQueueDao.resetExhausted() }
            val countBefore = runCatching { syncQueueDao.count() }.getOrDefault(0)
            val result = runCatching { syncRepo.runSync() }
            val countAfter = runCatching { syncQueueDao.count() }.getOrDefault(-1)
            val msg = result.fold(
                onSuccess = { res ->
                    val processed = (countBefore - countAfter).coerceAtLeast(0)
                    when (res) {
                        SyncResult.Success       -> "✓ OK — $processed op subidas, cola: $countAfter"
                        SyncResult.NoAuth        -> "✗ Sin sesión activa (inicia sesión otra vez)"
                        SyncResult.Unauthorized  -> "✗ Token rechazado por el servidor (401)"
                        SyncResult.UploadError   -> "✗ Subida falló — server no recibió. Cola: $countBefore→$countAfter"
                        SyncResult.DownloadError -> "✓ Subida OK ($processed), descarga falló. Cola: $countAfter"
                    }
                },
                onFailure = { e -> "✗ Excepción: ${e.javaClass.simpleName}: ${e.message ?: "sin detalle"}" },
            )
            _ui.update { it.copy(isForceSyncing = false, forceSyncResult = msg) }
        }
    }

    fun clearForceSyncResult() = _ui.update { it.copy(forceSyncResult = null) }

    // ── Crear ruta ────────────────────────────────────────────
    fun onShowCreateDialog() {
        _ui.update { it.copy(showCreateDialog = true, selectedAssigneeId = null, assignableUsers = emptyList()) }
        loadAssignableUsers()
    }
    fun onDismissCreateDialog()         = _ui.update { it.copy(showCreateDialog = false, newRouteName = "", error = null, selectedAssigneeId = null, assignableUsers = emptyList()) }
    fun onNewRouteNameChange(v: String) = _ui.update { it.copy(newRouteName = v) }

    fun onSelectAssignee(userId: Int?) = _ui.update { it.copy(selectedAssigneeId = userId) }

    // ---- Modo selección múltiple / reasignación masiva ----

    fun enterSelectionMode() {
        loadAssignableUsers()
        _ui.update { it.copy(selectionMode = true, selectedRouteUids = emptySet()) }
    }

    fun exitSelectionMode() = _ui.update {
        it.copy(selectionMode = false, selectedRouteUids = emptySet(), showBulkDialog = false)
    }

    fun toggleRouteSelection(uid: String) = _ui.update {
        val next = if (uid in it.selectedRouteUids) it.selectedRouteUids - uid
                   else it.selectedRouteUids + uid
        it.copy(selectedRouteUids = next)
    }

    fun selectAllRoutes() = _ui.update {
        it.copy(selectedRouteUids = it.routes.map { r -> r.uid }.toSet())
    }

    fun clearSelection() = _ui.update { it.copy(selectedRouteUids = emptySet()) }

    fun onShowBulkDialog() = _ui.update {
        it.copy(showBulkDialog = true, bulkAssigneeId = null, bulkReason = "")
    }

    fun onDismissBulkDialog() = _ui.update {
        it.copy(showBulkDialog = false, bulkAssigneeId = null, bulkReason = "")
    }

    fun onBulkAssigneeChange(userId: Int?) = _ui.update { it.copy(bulkAssigneeId = userId) }
    fun onBulkReasonChange(v: String)      = _ui.update { it.copy(bulkReason = v) }

    fun reassignSelectedRoutes() {
        val uids   = _ui.value.selectedRouteUids.toList()
        val target = _ui.value.bulkAssigneeId
        if (uids.isEmpty()) { _ui.update { it.copy(error = "No hay rutas seleccionadas") }; return }
        if (target == null) { _ui.update { it.copy(error = "Selecciona un destinatario") }; return }
        viewModelScope.launch {
            _ui.update { it.copy(isBulkAssigning = true) }
            routeRepo.reassignRoutesBulk(
                routeUids = uids,
                newUserId = target,
                reason    = _ui.value.bulkReason.trim().ifBlank { null },
            ).fold(
                onSuccess = { count ->
                    _ui.update {
                        it.copy(
                            isBulkAssigning   = false,
                            showBulkDialog    = false,
                            selectionMode     = false,
                            selectedRouteUids = emptySet(),
                            error             = if (count > 0) null else "No se reasignó ninguna ruta",
                        )
                    }
                },
                onFailure = { t ->
                    _ui.update { it.copy(isBulkAssigning = false, error = t.message ?: "Error al reasignar") }
                },
            )
        }
    }

    private fun loadAssignableUsers() {
        val role = session.userRole
        // solo roles que pueden asignar a inferiores necesitan la lista
        if (!UserRole.from(role).canCreateRoutes) return
        viewModelScope.launch {
            _ui.update { it.copy(loadingUsers = true) }
            when (val r = adminRepo.listUsers()) {
                is com.pabl3st.rutapp.data.repository.AuthResult.Success -> {
                    val myRole = session.userRole
                    val myId   = session.userId
                    // Filtrar: solo usuarios activos de rol inferior que reportan a mí o a mi equipo
                    val assignable = r.data.filter { u ->
                        u.isActive && u.userId != myId && when (myRole) {
                            "god"   -> UserRole.from(u.role).level >= UserRole.AGENT.level
                            "owner" -> UserRole.from(u.role) in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.AGENT)
                            "admin" -> UserRole.from(u.role) in listOf(UserRole.MANAGER, UserRole.AGENT)
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
        val date = java.time.LocalDate.now().toString()
        viewModelScope.launch {
            routeRepo.createRoute(
                name         = name,
                dateAssigned = date,
                forUserId    = _ui.value.selectedAssigneeId,
            )
            _ui.update { it.copy(showCreateDialog = false, newRouteName = "", selectedAssigneeId = null) }
        }
    }

    fun syncNow() {
        if (_ui.value.isSyncing) return
        viewModelScope.launch {
            _ui.update { it.copy(isSyncing = true) }
            // Pull-to-refresh manual → full-sync: el usuario espera ver TODO
            // actualizado, incluidos cambios hechos directamente en BD.
            routeRepo.fetchDelta(forceFull = true)
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

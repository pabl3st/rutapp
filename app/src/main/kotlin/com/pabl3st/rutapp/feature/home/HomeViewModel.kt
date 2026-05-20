package com.pabl3st.rutapp.feature.home

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.repository.SyncRepository
import com.pabl3st.rutapp.data.session.SessionManager
import com.pabl3st.rutapp.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Modelo de ruta enriquecido con progreso de paradas ────────
data class RouteWithProgress(
    val route:            RouteEntity,
    val totalStops:       Int          = 0,
    val doneStops:        Int          = 0,
    val progress:         Float        = 0f,   // 0.0 – 1.0
    val nextPendingStop:  StopEntity?  = null, // primera parada pendiente
)

data class HomeUiState(
    val routes:         List<RouteWithProgress> = emptyList(),
    val totalStops:     Int     = 0,   // suma de todas las rutas de hoy
    val doneStops:      Int     = 0,   // suma de visitadas hoy
    val pendingStops:   Int     = 0,   // suma de pendientes hoy
    val isLoading:      Boolean = true,
    val isSyncing:      Boolean = false,
    val userName:       String  = "",
    val userRole:       String  = "agent",
    val isManager:      Boolean = false,  // owner/admin/manager/god
    val error:          String? = null,
)

private val MANAGER_ROLES = setOf("owner", "admin", "manager", "god")

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val routeRepo:   RouteRepository,
    private val stopRepo:    StopRepository,
    private val syncRepo:    SyncRepository,
    private val session:     SessionManager,
    private val workManager: WorkManager,
) : BaseViewModel() {

    private val _ui = MutableStateFlow(
        HomeUiState(
            userName  = session.userDisplayName,
            userRole  = session.userRole,
            isManager = session.userRole in MANAGER_ROLES,
        )
    )
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    // Job para las suscripciones de stops — se cancela cuando cambian las rutas
    private var stopsJob: kotlinx.coroutines.Job? = null

    init {
        observeRoutes()
        schedulePeriodicSync()
        syncNow()
    }

    private fun observeRoutes() {
        viewModelScope.launch {
            // Managers ven todas las rutas del account; agents solo las suyas
            val routeFlow = routeRepo.observeToday()

            routeFlow
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { routes ->
                    _ui.update { it.copy(isLoading = false) }
                    subscribeStops(routes)
                }
        }
    }

    // ── Para cada ruta, observa sus stops y calcula progreso ──
    private fun subscribeStops(routes: List<RouteEntity>) {
        stopsJob?.cancel()
        if (routes.isEmpty()) {
            _ui.update { it.copy(routes = emptyList(), totalStops = 0, doneStops = 0, pendingStops = 0) }
            return
        }
        stopsJob = viewModelScope.launch {
            val routeUids = routes.map { it.uid }
            stopRepo.observeByRouteUids(routeUids)
                .catch { e -> _ui.update { it.copy(error = e.message) } }
                .collect { allStops ->
                    val stopsByRoute = allStops.groupBy { it.routeUid }
                    val enriched = routes.map { route ->
                        val stops   = stopsByRoute[route.uid] ?: emptyList()
                        val done    = stops.count { it.status == "done" }
                        val total   = stops.size
                        RouteWithProgress(
                            route           = route,
                            totalStops      = total,
                            doneStops       = done,
                            progress        = if (total > 0) done.toFloat() / total else 0f,
                            nextPendingStop = stops
                                .filter { it.status == "pending" || it.status == "visiting" }
                                .minByOrNull { it.orderIndex },
                        )
                    }
                    val totalAll   = enriched.sumOf { it.totalStops }
                    val doneAll    = enriched.sumOf { it.doneStops }
                    _ui.update { it.copy(
                        routes       = enriched,
                        totalStops   = totalAll,
                        doneStops    = doneAll,
                        pendingStops = totalAll - doneAll,
                    )}
                }
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

    private fun schedulePeriodicSync() {
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            SyncWorker.periodicRequest(),
        )
    }

    fun clearError() = _ui.update { it.copy(error = null) }
    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(
            isLoading = false,
            error     = t.message ?: "Error inesperado",
        )}
    }

}

package com.pabl3st.rutapp.feature.home

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

// ── Resumen de paradas por ruta — calculado en VM ─────────────
data class RouteProgress(
    val route:    RouteEntity,
    val total:    Int = 0,
    val done:     Int = 0,
    val visiting: Int = 0,
) {
    val pending: Int   get() = total - done
    val pct:     Float get() = if (total > 0) done.toFloat() / total else 0f
}

data class HomeUiState(
    val routes:        List<RouteProgress> = emptyList(),
    val isLoading:     Boolean             = true,
    val isSyncing:     Boolean             = false,
    val userName:      String              = "",
    val userRole:      String              = "agent",
    val error:         String?             = null,
    val totalRoutes:   Int                 = 0,
    val doneStops:     Int                 = 0,
    val pendingStops:  Int                 = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val routeRepo:   RouteRepository,
    private val stopRepo:    StopRepository,
    private val syncRepo:    SyncRepository,
    private val session:     SessionManager,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        HomeUiState(
            userName = session.userDisplayName,
            userRole = session.userRole,
        )
    )
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    private var stopsJob: kotlinx.coroutines.Job? = null

    init {
        observeRoutes()
        schedulePeriodicSync()
        syncNow()
    }

    private fun observeRoutes() {
        viewModelScope.launch {
            routeRepo.observeToday()
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { routes -> subscribeStops(routes) }
        }
    }

    private fun subscribeStops(routes: List<RouteEntity>) {
        stopsJob?.cancel()
        if (routes.isEmpty()) {
            _ui.update { it.copy(
                isLoading    = false,
                routes       = emptyList(),
                totalRoutes  = 0,
                doneStops    = 0,
                pendingStops = 0,
            )}
            return
        }
        stopsJob = viewModelScope.launch {
            stopRepo.observeByRouteUids(routes.map { it.uid })
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { allStops ->
                    val byRoute  = allStops.groupBy { it.routeUid }
                    val progress = routes.map { route ->
                        val stops = byRoute[route.uid] ?: emptyList()
                        RouteProgress(
                            route    = route,
                            total    = stops.size,
                            done     = stops.count { it.status == "done" },
                            visiting = stops.count { it.status == "visiting" },
                        )
                    }
                    _ui.update { it.copy(
                        routes       = progress,
                        isLoading    = false,
                        totalRoutes  = routes.size,
                        doneStops    = progress.sumOf { p -> p.done },
                        pendingStops = progress.sumOf { p -> p.pending },
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
}

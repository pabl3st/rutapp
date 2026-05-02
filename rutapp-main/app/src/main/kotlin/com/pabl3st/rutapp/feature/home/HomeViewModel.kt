package com.pabl3st.rutapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.repository.JornadaRepository
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.repository.SyncRepository
import com.pabl3st.rutapp.data.session.SessionManager
import com.pabl3st.rutapp.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val routes: List<RouteEntity> = emptyList(),
    val isLoading: Boolean        = true,
    val isSyncing: Boolean        = false,
    val pendingSync: Int          = 0,
    val lastSync: String          = "",
    val userName: String          = "",
    // ── Resumen del día ─────────────────────────────────
    val totalStopsToday: Int      = 0,
    val doneStopsToday: Int       = 0,
    val distanceKmToday: Double   = 0.0,  // suma de todas las jornadas activas hoy
    val error: String?            = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val routeRepo:    RouteRepository,
    private val stopRepo:     StopRepository,
    private val jornadaRepo:  JornadaRepository,
    private val syncRepo:     SyncRepository,
    private val session:      SessionManager,
    private val workManager:  WorkManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        HomeUiState(userName = session.userDisplayName)
    )
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        observeRoutes()
        observeDayStats()
        schedulePeriodicSync()
        syncNow()
    }

    private fun observeDayStats() {
        viewModelScope.launch {
            // Recalcular stats cada vez que cambian las rutas del día
            routeRepo.observeToday().collect { routes ->
                val routeUids = routes.map { it.uid }
                if (routeUids.isEmpty()) {
                    _ui.update { it.copy(totalStopsToday = 0, doneStopsToday = 0, distanceKmToday = 0.0) }
                    return@collect
                }
                val stops      = stopRepo.observeByRouteUids(routeUids)
                // Obtener snapshot actual de stops
                stops.collect { list ->
                    val total      = list.size
                    val done       = list.count { it.status == "done" }
                    val dateStr    = jornadaRepo.todayStr()
                    val distanceKm = routeUids.sumOf { uid ->
                        jornadaRepo.get(uid, dateStr)?.distanceKm ?: 0.0
                    }
                    _ui.update { it.copy(
                        totalStopsToday = total,
                        doneStopsToday  = done,
                        distanceKmToday = distanceKm,
                    )}
                }
            }
        }
    }

    private fun observeRoutes() {
        viewModelScope.launch {
            routeRepo.observeToday()
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { routes ->
                    _ui.update { it.copy(routes = routes, isLoading = false) }
                }
        }
    }

    fun syncNow() {
        if (_ui.value.isSyncing) return
        viewModelScope.launch {
            _ui.update { it.copy(isSyncing = true) }
            routeRepo.fetchDelta()
            val pending = syncRepo.pendingCount()
            _ui.update { it.copy(
                isSyncing   = false,
                pendingSync = pending,
                lastSync    = session.lastSyncTimestamp,
            )}
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

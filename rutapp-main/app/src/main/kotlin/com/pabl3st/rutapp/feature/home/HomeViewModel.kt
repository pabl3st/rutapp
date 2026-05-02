package com.pabl3st.rutapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
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
    // Ruta del día (1 ruta por día)
    val route:         RouteEntity?    = null,
    // Paradas de la ruta — lista principal de la pantalla
    val stops:         List<StopEntity> = emptyList(),
    val isLoading:     Boolean          = true,
    val isSyncing:     Boolean          = false,
    val pendingSync:   Int              = 0,
    val lastSync:      String           = "",
    val userName:      String           = "",
    // Resumen del día
    val totalStops:    Int              = 0,
    val doneStops:     Int              = 0,
    val distanceKm:    Double           = 0.0,
    val error:         String?          = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val routeRepo:   RouteRepository,
    private val stopRepo:    StopRepository,
    private val jornadaRepo: JornadaRepository,
    private val syncRepo:    SyncRepository,
    private val session:     SessionManager,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState(userName = session.userDisplayName))
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    private var stopsJob: kotlinx.coroutines.Job? = null

    init {
        observeRoute()
        schedulePeriodicSync()
        syncNow()
    }

    // ── Observa la ruta del día → cuando llega, suscribe sus stops ──
    private fun observeRoute() {
        viewModelScope.launch {
            routeRepo.observeToday()
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { routes ->
                    val route = routes.firstOrNull()
                    _ui.update { it.copy(route = route, isLoading = false) }
                    if (route != null) {
                        subscribeStops(route.uid)
                    } else {
                        stopsJob?.cancel()
                        _ui.update { it.copy(stops = emptyList(), totalStops = 0, doneStops = 0) }
                    }
                }
        }
    }

    private fun subscribeStops(routeUid: String) {
        stopsJob?.cancel()
        stopsJob = viewModelScope.launch {
            stopRepo.observeByRoute(routeUid)
                .catch { e -> _ui.update { it.copy(error = e.message) } }
                .collect { stops ->
                    val done       = stops.count { it.status == "done" }
                    val distanceKm = jornadaRepo.get(routeUid, jornadaRepo.todayStr())?.distanceKm ?: 0.0
                    _ui.update { it.copy(
                        stops      = stops,
                        totalStops = stops.size,
                        doneStops  = done,
                        distanceKm = distanceKm,
                    )}
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

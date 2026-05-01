package com.pabl3st.rutapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
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
    val error: String?            = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val routeRepo:   RouteRepository,
    private val syncRepo:    SyncRepository,
    private val session:     SessionManager,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        HomeUiState(userName = session.userDisplayName)
    )
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        observeRoutes()
        schedulePeriodicSync()
        syncNow()
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

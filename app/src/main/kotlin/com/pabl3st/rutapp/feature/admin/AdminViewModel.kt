package com.pabl3st.rutapp.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.repository.SyncRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val userName:      String  = "",
    val userEmail:     String  = "",
    val userRole:      String  = "",
    val accountName:   String  = "",
    val accountPlan:   String  = "",
    // Estadísticas globales (solo god/admin)
    val totalRoutes:   Int     = 0,
    val totalStops:    Int     = 0,
    val pendingSync:   Int     = 0,
    val isLoading:     Boolean = true,
    val error:         String? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val session:   SessionManager,
    private val syncRepo:  SyncRepository,
    private val routeRepo: RouteRepository,
    private val stopRepo:  StopRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AdminUiState(
        userName    = session.userDisplayName,
        userEmail   = session.userEmail,
        userRole    = session.userRole,
        accountName = session.accountName,
    ))
    val ui: StateFlow<AdminUiState> = _ui.asStateFlow()

    init { loadStats() }

    private fun loadStats() {
        viewModelScope.launch {
            val pending = syncRepo.pendingCount()
            routeRepo.observeAll()
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { routes ->
                    val routeUids = routes.map { it.uid }
                    val stops = stopRepo.observeByRouteUids(routeUids)
                    stops.collect { stopList ->
                        _ui.update { it.copy(
                            totalRoutes = routes.size,
                            totalStops  = stopList.size,
                            pendingSync = pending,
                            isLoading   = false,
                        )}
                    }
                }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

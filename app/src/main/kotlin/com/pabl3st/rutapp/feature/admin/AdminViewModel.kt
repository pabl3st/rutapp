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
    val userName:    String  = "",
    val userEmail:   String  = "",
    val userRole:    String  = "",
    val accountName: String  = "",
    val accountType: String  = "",
    val totalRoutes: Int     = 0,
    val totalStops:  Int     = 0,
    val pendingSync: Int     = 0,
    val isLoading:   Boolean = true,
    val error:       String? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val session:   SessionManager,
    private val syncRepo:  SyncRepository,
    private val routeRepo: RouteRepository,
    private val stopRepo:  StopRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        AdminUiState(
            userName    = session.userDisplayName.ifBlank { session.userName },
            userEmail   = session.userEmail,
            userRole    = session.userRole,
            accountName = session.accountName,
            accountType = session.accountType,
        )
    )
    val ui: StateFlow<AdminUiState> = _ui.asStateFlow()

    init { loadStats() }

    private fun loadStats() {
        viewModelScope.launch {
            val pending = syncRepo.pendingCount()

            // Combinar rutas + stops en un solo Flow para evitar collect anidado
            routeRepo.observeAll()
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .flatMapLatest { routes ->
                    val uids = routes.map { it.uid }
                    stopRepo.observeByRouteUids(uids).map { stops ->
                        routes.size to stops.size
                    }
                }
                .collect { (routeCount, stopCount) ->
                    _ui.update { it.copy(
                        totalRoutes = routeCount,
                        totalStops  = stopCount,
                        pendingSync = pending,
                        isLoading   = false,
                    ) }
                }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

package com.pabl3st.rutapp.feature.rutas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteDetailUiState(
    val route: RouteEntity?     = null,
    val stops: List<StopEntity> = emptyList(),
    val isLoading: Boolean      = true,
    val error: String?          = null,
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepo: RouteRepository,
    private val stopRepo:  StopRepository,
) : ViewModel() {

    private val routeUid: String = checkNotNull(savedStateHandle["routeUid"])

    private val _ui = MutableStateFlow(RouteDetailUiState())
    val ui: StateFlow<RouteDetailUiState> = _ui.asStateFlow()

    init {
        loadRoute()
        observeStops()
    }

    private fun loadRoute() {
        viewModelScope.launch {
            val route = routeRepo.getByUid(routeUid)
            _ui.update { it.copy(route = route, isLoading = false) }
        }
    }

    private fun observeStops() {
        viewModelScope.launch {
            stopRepo.observeByRoute(routeUid)
                .catch { e -> _ui.update { it.copy(error = e.message) } }
                .collect { stops -> _ui.update { it.copy(stops = stops) } }
        }
    }

    fun markStopVisited(uid: String) {
        viewModelScope.launch { stopRepo.markVisited(uid) }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

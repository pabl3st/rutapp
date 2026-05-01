package com.pabl3st.rutapp.feature.rutas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.core.map.MapProvider
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteDetailUiState(
    val route: RouteEntity?        = null,
    val stops: List<StopEntity>    = emptyList(),
    val isLoading: Boolean         = true,
    val showAddStopDialog: Boolean  = false,
    val newStopName: String         = "",
    val newStopExternalId: String   = "",
    val newStopAddress: String      = "",
    val error: String?              = null,
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepo:   RouteRepository,
    private val stopRepo:    StopRepository,
    private val mapProvider: MapProvider,
) : ViewModel() {

    // El nombre "routeUid" debe coincidir exactamente con el navArgument en RutasNavGraph
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

    // ── Añadir stop ───────────────────────────────────────────
    fun onShowAddStopDialog()             = _ui.update { it.copy(showAddStopDialog = true) }
    fun onDismissAddStopDialog()          = _ui.update { it.copy(showAddStopDialog = false, newStopName = "", newStopExternalId = "", newStopAddress = "", error = null) }
    fun onNewStopNameChange(v: String)       = _ui.update { it.copy(newStopName = v) }
    fun onNewStopExternalIdChange(v: String) = _ui.update { it.copy(newStopExternalId = v) }
    fun onNewStopAddressChange(v: String) = _ui.update { it.copy(newStopAddress = v) }

    fun addStop() {
        val name = _ui.value.newStopName.trim()
        if (name.isBlank()) {
            _ui.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            val address = _ui.value.newStopAddress.trim().ifEmpty { null }
            val stop = stopRepo.createStop(
                routeUid   = routeUid,
                name       = name,
                externalId = _ui.value.newStopExternalId.trim().ifEmpty { null },
                address    = address,
                orderIndex = _ui.value.stops.size,
            )
            _ui.update { it.copy(showAddStopDialog = false, newStopName = "", newStopExternalId = "", newStopAddress = "") }
            // Geocodificar en background — no bloquea el cierre del diálogo
            if (!address.isNullOrBlank()) {
                viewModelScope.launch {
                    stopRepo.geocodeAddress(stop.uid, address, mapProvider::geocode)
                }
            }
        }
    }

    fun markStopVisited(uid: String) {
        viewModelScope.launch { stopRepo.markVisited(uid) }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

package com.pabl3st.rutapp.feature.rutas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.pabl3st.rutapp.core.location.LocationManager
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StopWithDistance(
    val stop: StopEntity,
    val distanceMeters: Float?,    // null si stop sin coords o sin GPS
    val distanceLabel: String,     // "120 m" / "2.3 km" / "Sin GPS"
)

data class RouteMapUiState(
    val stops: List<StopWithDistance>      = emptyList(),
    val routeName: String                  = "",
    val userLocation: LatLng?              = null,
    val isLocating: Boolean                = false,
    val locationPermissionGranted: Boolean = false,
    val showPermissionRationale: Boolean   = false,
    val error: String?                     = null,
)

@HiltViewModel
class RouteMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepo:   RouteRepository,
    private val stopRepo:    StopRepository,
    private val locationMgr: LocationManager,
) : ViewModel() {

    // Nombre del navArgument debe coincidir exactamente con Screen.RouteMap.route
    private val routeUid: String = checkNotNull(savedStateHandle["routeUid"])

    private val _ui = MutableStateFlow(RouteMapUiState())
    val ui: StateFlow<RouteMapUiState> = _ui.asStateFlow()

    init {
        loadRouteName()
        observeStops()
    }

    private fun loadRouteName() {
        viewModelScope.launch {
            routeRepo.getByUid(routeUid)?.let { route ->
                _ui.update { it.copy(routeName = route.name) }
            }
        }
    }

    private fun observeStops() {
        viewModelScope.launch {
            stopRepo.observeByRoute(routeUid)
                .catch { e -> _ui.update { it.copy(error = e.message) } }
                .collect { stops -> recalculateDistances(stops) }
        }
    }

    // ── Permiso GPS ───────────────────────────────────────────
    fun onPermissionGranted() {
        _ui.update { it.copy(locationPermissionGranted = true, showPermissionRationale = false) }
        startLocationUpdates()
    }

    fun onPermissionDenied() {
        _ui.update { it.copy(showPermissionRationale = true) }
    }

    fun dismissPermissionRationale() {
        _ui.update { it.copy(showPermissionRationale = false) }
    }

    // ── Localización ──────────────────────────────────────────
    private fun startLocationUpdates() {
        viewModelScope.launch {
            _ui.update { it.copy(isLocating = true) }

            // Posición inmediata desde caché (sin esperar fix GPS)
            locationMgr.getLastLocation()?.let { loc ->
                val ll = LatLng(loc.latitude, loc.longitude)
                _ui.update { it.copy(userLocation = ll, isLocating = false) }
                recalculateDistances(_ui.value.stops.map { it.stop })
            }

            // Actualizaciones continuas mientras el mapa esté abierto
            locationMgr.locationUpdates().collect { loc ->
                val ll = LatLng(loc.latitude, loc.longitude)
                _ui.update { it.copy(userLocation = ll, isLocating = false) }
                recalculateDistances(_ui.value.stops.map { it.stop })
            }
        }
    }

    // ── Distancias ────────────────────────────────────────────
    private fun recalculateDistances(stops: List<StopEntity>) {
        val userLoc = _ui.value.userLocation
        val withDist = stops.map { stop ->
            val dist = if (userLoc != null && stop.lat != null && stop.lng != null)
                locationMgr.distanceBetween(userLoc.latitude, userLoc.longitude, stop.lat, stop.lng)
            else null

            StopWithDistance(
                stop           = stop,
                distanceMeters = dist,
                distanceLabel  = dist?.let { formatDistance(it) } ?: "Sin GPS",
            )
        }.sortedWith(
            // Pendientes primero ordenados por distancia, después los completados
            compareBy(
                { if (it.stop.status == "done") 1 else 0 },
                { it.distanceMeters ?: Float.MAX_VALUE },
            )
        )
        _ui.update { it.copy(stops = withDist) }
    }

    private fun formatDistance(meters: Float): String = when {
        meters < 1000 -> "${meters.toInt()} m"
        else          -> "${"%.1f".format(meters / 1000)} km"
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

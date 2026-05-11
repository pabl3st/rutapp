package com.pabl3st.rutapp.feature.rutas

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.location.LocationManager
import com.pabl3st.rutapp.core.map.MapConfig
import com.pabl3st.rutapp.core.map.MapLatLng
import com.pabl3st.rutapp.core.map.MapProvider
import com.pabl3st.rutapp.core.map.RouteOptions
import com.pabl3st.rutapp.core.map.StopMapMarker
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteMapUiState(
    val stops: List<StopMapMarker>         = emptyList(),
    val routeName: String                  = "",
    val userLocation: MapLatLng?           = null,
    val isLocating: Boolean                = false,
    val locationPermissionGranted: Boolean = false,
    val showPermissionRationale: Boolean   = false,
    val isOptimizing: Boolean              = false,
    val routePolyline: List<MapLatLng>     = emptyList(),
    val error: String?                     = null,
)

@HiltViewModel
class RouteMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepo:   RouteRepository,
    private val stopRepo:    StopRepository,
    private val locationMgr: LocationManager,
    val mapProvider: MapProvider,     // expuesto para que Screen lo use directamente
    val mapConfig: MapConfig,
) : BaseViewModel() {

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
                .collect { stops -> recalculate(stops) }
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

    // ── GPS ───────────────────────────────────────────────────
    private fun startLocationUpdates() {
        viewModelScope.launch {
            _ui.update { it.copy(isLocating = true) }
            locationMgr.getLastLocation()?.let { loc ->
                val ll = MapLatLng(loc.latitude, loc.longitude)
                _ui.update { it.copy(userLocation = ll, isLocating = false) }
                recalculate(_ui.value.stops.map { it.toStopEntity() })
            }
            locationMgr.locationUpdates().collect { loc ->
                val ll = MapLatLng(loc.latitude, loc.longitude)
                _ui.update { it.copy(userLocation = ll, isLocating = false) }
                recalculate(_ui.value.stops.map { it.toStopEntity() })
            }
        }
    }

    // ── Ordenar por cercanía (nearest neighbor) ───────────────
    fun optimizeOrder() {
        val userLoc = _ui.value.userLocation ?: return
        viewModelScope.launch {
            _ui.update { it.copy(isOptimizing = true) }
            val optimized = mapProvider.optimizeStopOrder(
                origin  = userLoc,
                stops   = _ui.value.stops,
                options = mapConfig.route,
            )
            _ui.update { it.copy(stops = optimized, isOptimizing = false) }
        }
    }

    // ── Recalcular distancias y reconstruir markers ───────────
    private fun recalculate(stops: List<StopEntity>) {
        val userLoc = _ui.value.userLocation
        val markers = stops
            .filter { it.lat != null && it.lng != null || true }
            .mapIndexed { index, stop ->
                val dist = if (userLoc != null && stop.lat != null && stop.lng != null)
                    locationMgr.distanceBetween(userLoc.lat, userLoc.lng, stop.lat, stop.lng)
                else null

                StopMapMarker(
                    uid           = stop.uid,
                    name          = stop.name,
                    externalId    = stop.externalId,
                    latLng        = MapLatLng(stop.lat ?: 0.0, stop.lng ?: 0.0),
                    status        = stop.status,
                    distanceLabel = dist?.let { formatDistance(it) } ?: "Sin GPS",
                    orderIndex    = stop.orderIndex,
                )
            }
            .sortedWith(
                compareBy(
                    { if (it.status == "done") 1 else 0 },
                    { stops.find { s -> s.uid == it.uid }?.let { s ->
                        if (userLoc != null && s.lat != null && s.lng != null)
                            locationMgr.distanceBetween(userLoc.lat, userLoc.lng, s.lat, s.lng).toDouble()
                        else Double.MAX_VALUE
                    } ?: Double.MAX_VALUE }
                )
            )

        _ui.update { it.copy(stops = markers) }

        // Calcular ruta OSRM si hay 2+ stops con GPS
        val withGps = markers.filter { it.latLng.lat != 0.0 && it.latLng.lng != 0.0 }
        if (withGps.size >= 2) {
            viewModelScope.launch { fetchRoute(withGps) }
        }
    }

    private suspend fun fetchRoute(markers: List<StopMapMarker>) {
        val origin = _ui.value.userLocation ?: markers.first().latLng
        val destinations = markers.map { it.latLng }
        val result = runCatching {
            mapProvider.calculateRoute(origin, destinations, mapConfig.route)
        }.getOrNull()
        result?.let { route ->
            _ui.update { it.copy(routePolyline = route.points) }
        }
    }

    private fun formatDistance(meters: Float): String = when {
        meters < 1000 -> "${meters.toInt()} m"
        else          -> "${"%.1f".format(meters / 1000)} km"
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

// Extensión inversa para pasar entidades cuando sea necesario
private fun StopMapMarker.toStopEntity() = com.pabl3st.rutapp.data.local.entity.StopEntity(
    uid        = uid,
    routeUid   = "",   // no necesario para recalcular
    accountId  = 0,
    name       = name,
    externalId = externalId,
    lat        = latLng.lat,
    lng        = latLng.lng,
    status     = status,
    orderIndex = orderIndex,
    createdAt  = "",
    updatedAt  = "",
)

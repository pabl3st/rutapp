package com.pabl3st.rutapp.feature.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.location.LocationManager
import com.pabl3st.rutapp.core.map.MapConfig
import com.pabl3st.rutapp.core.map.MapLatLng
import com.pabl3st.rutapp.core.map.MapProvider
import com.pabl3st.rutapp.core.map.RouteOptions
import com.pabl3st.rutapp.core.map.StopMapMarker
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// Filtro de estado sobre el mapa global
// Escalable: añadir PRIORITY_HIGH, SEGMENT_A, etc. aquí
// ─────────────────────────────────────────────────────────────
enum class MapStatusFilter {
    ALL,        // todos los stops del día
    PENDING,    // pendientes + en visita
    DONE,       // completados
    NO_GPS,     // sin coordenadas (para geocodificación pendiente)
}

// ─────────────────────────────────────────────────────────────
// UiState inmutable — single source of truth para la pantalla
// ─────────────────────────────────────────────────────────────
data class GlobalMapUiState(

    // Datos en bruto
    val routes:         List<RouteEntity>      = emptyList(),
    val allStops:       List<StopMapMarker>    = emptyList(),
    val stopRouteIndex: Map<String, String>    = emptyMap(), // stopUid -> routeUid

    // Vista filtrada — lo que renderiza el mapa y la lista
    val visibleStops: List<StopMapMarker> = emptyList(),
    val routePolyline: List<MapLatLng>   = emptyList(),

    // Estadísticas del día
    val statTotal:   Int = 0,
    val statDone:    Int = 0,
    val statPending: Int = 0,
    val statNoGps:   Int = 0,

    // GPS
    val userLocation: MapLatLng? = null,
    val isLocating:   Boolean    = false,

    // Filtros activos
    val activeStatusFilter:  MapStatusFilter = MapStatusFilter.ALL,
    val selectedRouteUid:    String?         = null,   // null = todas las rutas

    // Permisos
    val locationPermissionGranted: Boolean = false,
    val showPermissionRationale:   Boolean = false,

    // Estado de carga
    val isLoading: Boolean = true,
    val error:     String? = null,
)

// ─────────────────────────────────────────────────────────────
// ViewModel del Mapa Global
// Observa rutas del día → agrega stops de todas → aplica filtros
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class GlobalMapViewModel @Inject constructor(
    private val routeRepo:   RouteRepository,
    private val stopRepo:    StopRepository,
    private val locationMgr: LocationManager,
    val mapProvider: MapProvider,   // expuesto: Screen lo usa directamente
    val mapConfig:   MapConfig,
) : ViewModel() {

    private val _ui = MutableStateFlow(GlobalMapUiState())
    val ui: StateFlow<GlobalMapUiState> = _ui.asStateFlow()

    // Job cancelable al cambiar lista de rutas
    private var stopsJob: Job? = null

    init {
        observeRoutesAndStops()
    }

    // ── Capa 1: observar rutas del día ────────────────────────
    // Cuando cambian las rutas → re-suscribir stops
    private fun observeRoutesAndStops() {
        viewModelScope.launch {
            routeRepo.observeToday()
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { routes ->
                    _ui.update { it.copy(routes = routes) }
                    resubscribeStops(routes.map { it.uid })
                }
        }
    }

    // ── Capa 2: observar stops de las rutas actuales ──────────
    // Cancela la suscripción anterior al recibir una lista nueva de rutas
    private fun resubscribeStops(routeUids: List<String>) {
        stopsJob?.cancel()
        stopsJob = viewModelScope.launch {
            stopRepo.observeByRouteUids(routeUids)
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { stops -> processStops(stops) }
        }
    }

    // ── Capa 3: convertir StopEntity → StopMapMarker + stats ─
    private fun processStops(stops: List<StopEntity>) {
        val userLoc   = _ui.value.userLocation
        val markers   = stops.map { it.toMarker(userLoc) }
        val index     = stops.associate { it.uid to it.routeUid }
        val filtered  = applyFilters(markers, stopRouteIndex = index)

        _ui.update { s ->
            s.copy(
                allStops        = markers,
                stopRouteIndex  = index,
                visibleStops    = filtered,
                statTotal    = markers.size,
                statDone     = markers.count { it.status == "done" },
                statPending  = markers.count { it.status == "pending" || it.status == "visiting" },
                statNoGps    = stops.count { it.lat == null || it.lat == 0.0 },
                isLoading    = false,
            )
        }
        val withGps = markers.filter { it.latLng.lat != 0.0 && it.latLng.lng != 0.0 }
        if (withGps.size >= 2) viewModelScope.launch { fetchRoutePolyline(withGps) }
    }

    private suspend fun fetchRoutePolyline(markers: List<StopMapMarker>) {
        val origin = _ui.value.userLocation ?: markers.first().latLng
        val result = runCatching {
            mapProvider.calculateRoute(origin, markers.map { it.latLng }, mapConfig.route)
        }.getOrNull()
        result?.let { route ->
            _ui.update { it.copy(routePolyline = route.points) }
        }
    }

    // ── StopEntity → StopMapMarker ────────────────────────────
    private fun StopEntity.toMarker(userLoc: MapLatLng?): StopMapMarker {
        val hasGps = lat != null && lng != null && lat != 0.0 && lng != 0.0
        val dist   = if (hasGps && userLoc != null)
            locationMgr.distanceBetween(userLoc.lat, userLoc.lng, lat!!, lng!!)
        else null

        return StopMapMarker(
            uid           = uid,
            name          = name,
            externalId    = externalId,
            latLng        = MapLatLng(lat ?: 0.0, lng ?: 0.0),
            status        = status,
            distanceLabel = dist?.formatAsDistance() ?: if (hasGps) "—" else "Sin GPS",
            orderIndex    = orderIndex,
        )
    }

    // ── Aplicar filtros de ruta + estado ──────────────────────
    private fun applyFilters(
        stops:          List<StopMapMarker>,
        statusFilter:   MapStatusFilter        = _ui.value.activeStatusFilter,
        routeUid:       String?                = _ui.value.selectedRouteUid,
        stopRouteIndex: Map<String, String>    = _ui.value.stopRouteIndex,
    ): List<StopMapMarker> {
        // Filtrar por ruta usando el índice uid→routeUid
        val byRoute = if (routeUid != null)
            stops.filter { stopRouteIndex[it.uid] == routeUid }
        else stops

        val byStatus = when (statusFilter) {
            MapStatusFilter.ALL     -> byRoute
            MapStatusFilter.PENDING -> byRoute.filter { it.status == "pending" || it.status == "visiting" }
            MapStatusFilter.DONE    -> byRoute.filter { it.status == "done" }
            MapStatusFilter.NO_GPS  -> byRoute.filter { it.latLng.lat == 0.0 || it.latLng.lng == 0.0 }
        }
        return byStatus.sortedWith(
            compareBy(
                { if (it.status == "done") 1 else 0 },     // done al final
                { it.latLng.lat == 0.0 },                   // sin GPS al final
                { it.orderIndex },                           // por orden dentro de ruta
            )
        )
    }

    // ── Filtros públicos ──────────────────────────────────────
    fun setStatusFilter(filter: MapStatusFilter) {
        val visible = applyFilters(_ui.value.allStops, filter, _ui.value.selectedRouteUid)
        _ui.update { it.copy(activeStatusFilter = filter, visibleStops = visible) }
    }

    fun setRouteFilter(routeUid: String?) {
        val visible = applyFilters(_ui.value.allStops, _ui.value.activeStatusFilter, routeUid)
        _ui.update { it.copy(selectedRouteUid = routeUid, visibleStops = visible) }
    }

    // ── GPS ───────────────────────────────────────────────────
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

    private fun startLocationUpdates() {
        viewModelScope.launch {
            _ui.update { it.copy(isLocating = true) }

            // Posición inmediata (caché)
            locationMgr.getLastLocation()?.let { loc ->
                onNewLocation(MapLatLng(loc.latitude, loc.longitude))
            }

            // Actualizaciones continuas — se cancela con el ViewModel
            locationMgr.locationUpdates().collect { loc ->
                onNewLocation(MapLatLng(loc.latitude, loc.longitude))
            }
        }
    }

    private fun onNewLocation(loc: MapLatLng) {
        _ui.update { it.copy(userLocation = loc, isLocating = false) }
        // Recalcular distancias directamente sobre los markers existentes
        // sin acceder a la BD — solo actualiza distanceLabel con la nueva posición
        val updated = _ui.value.allStops.map { marker ->
            val hasGps = marker.latLng.lat != 0.0 && marker.latLng.lng != 0.0
            val label  = if (hasGps)
                locationMgr.distanceBetween(loc.lat, loc.lng, marker.latLng.lat, marker.latLng.lng)
                    .formatAsDistance()
            else "Sin GPS"
            marker.copy(distanceLabel = label)
        }
        val filtered = applyFilters(updated)
        _ui.update { it.copy(allStops = updated, visibleStops = filtered) }
    }

    fun clearError() = _ui.update { it.copy(error = null) }

    // ── Extensión local: Float metros → label ─────────────────
    private fun Float.formatAsDistance(): String = when {
        this < 1_000f -> "${toInt()} m"
        else          -> "${"%.1f".format(this / 1_000f)} km"
    }
}

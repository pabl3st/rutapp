package com.pabl3st.rutapp.feature.rutas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.location.LocationManager
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

enum class StopSortMode { MANUAL, GPS, GREEDY }

data class RouteDetailUiState(
    val route: RouteEntity?     = null,
    val stops: List<StopEntity> = emptyList(),
    val sortMode: StopSortMode  = StopSortMode.MANUAL,
    val isReordering: Boolean   = false,
    val isLoading: Boolean      = true,
    val error: String?          = null,
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepo:    RouteRepository,
    private val stopRepo:     StopRepository,
    private val locationMgr:  LocationManager,
) : ViewModel() {

    private val routeUid: String = checkNotNull(savedStateHandle["routeUid"])

    private val _ui = MutableStateFlow(RouteDetailUiState())
    val ui: StateFlow<RouteDetailUiState> = _ui.asStateFlow()

    // Lista base de Room (siempre por orderIndex)
    private val _baseStops = MutableStateFlow<List<StopEntity>>(emptyList())

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
                .collect { stops ->
                    _baseStops.value = stops
                    applySortMode(_ui.value.sortMode, stops)
                }
        }
    }

    fun setSortMode(mode: StopSortMode) {
        _ui.update { it.copy(sortMode = mode) }
        viewModelScope.launch {
            applySortMode(mode, _baseStops.value)
        }
    }

    private suspend fun applySortMode(mode: StopSortMode, stops: List<StopEntity>) {
        val sorted = when (mode) {
            StopSortMode.MANUAL  -> stops
            StopSortMode.GPS     -> sortByGps(stops)
            StopSortMode.GREEDY  -> sortGreedy(stops)
        }
        _ui.update { it.copy(stops = sorted) }
    }

    // ── Nearest-neighbor desde posición GPS actual ────────────
    private suspend fun sortByGps(stops: List<StopEntity>): List<StopEntity> {
        val loc = locationMgr.getLastLocation() ?: return stops
        val withGps = stops.filter { it.lat != null && it.lng != null }
        val withoutGps = stops.filter { it.lat == null || it.lng == null }
        val sorted = withGps.sortedBy { haversine(loc.latitude, loc.longitude, it.lat!!, it.lng!!) }
        return sorted + withoutGps
    }

    // ── Greedy nearest-neighbor desde la primera parada ──────
    private fun sortGreedy(stops: List<StopEntity>): List<StopEntity> {
        val withGps = stops.filter { it.lat != null && it.lng != null }.toMutableList()
        val withoutGps = stops.filter { it.lat == null || it.lng == null }
        if (withGps.size <= 1) return stops

        val result = mutableListOf<StopEntity>()
        var current = withGps.removeAt(0)
        result.add(current)

        while (withGps.isNotEmpty()) {
            val next = withGps.minByOrNull { haversine(current.lat!!, current.lng!!, it.lat!!, it.lng!!) }!!
            withGps.remove(next)
            result.add(next)
            current = next
        }
        return result + withoutGps
    }

    // ── Persistir orden actual en Room ────────────────────────
    fun saveCurrentOrder() {
        viewModelScope.launch {
            _ui.update { it.copy(isReordering = true) }
            stopRepo.reorderStops(_ui.value.stops)
            _ui.update { it.copy(isReordering = false) }
        }
    }

    // markStopVisited eliminado — el marcado solo ocurre al guardar el formulario de visita

    fun clearError() = _ui.update { it.copy(error = null) }

    // ── Haversine en km ───────────────────────────────────────
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}


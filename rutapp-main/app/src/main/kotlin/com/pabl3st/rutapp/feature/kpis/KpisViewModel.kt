package com.pabl3st.rutapp.feature.kpis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// Período de tiempo para las métricas
// ─────────────────────────────────────────────────────────────
enum class KpiPeriod { TODAY, WEEK, MONTH }

// ─────────────────────────────────────────────────────────────
// Métricas calculadas para el período activo
// ─────────────────────────────────────────────────────────────
data class KpiMetrics(
    // Visitas
    val totalStops:      Int     = 0,
    val doneStops:       Int     = 0,
    val pendingStops:    Int     = 0,
    val skippedStops:    Int     = 0,
    val visitingStops:   Int     = 0,

    // Ratios
    val completionRate:  Float   = 0f,   // doneStops / totalStops
    val contactRate:     Float   = 0f,   // contactado / totalStops

    // GPS
    val stopsWithGps:    Int     = 0,
    val stopsWithoutGps: Int     = 0,
    val gpsRate:         Float   = 0f,

    // Rutas
    val totalRoutes:     Int     = 0,
    val activeRoutes:    Int     = 0,
    val doneRoutes:      Int     = 0,

    // Resultados de visita
    val resultContacted:  Int    = 0,    // visitResult = "contactado"
    val resultNotHome:    Int    = 0,    // visitResult = "no_estaba"
    val resultReturn:     Int    = 0,    // visitResult = "volvemos"
    val resultRejected:   Int    = 0,    // visitResult = "rechazado"

    // Tendencia (7 días) — lista de (fecha, nStops) para mini-chart
    val weeklyTrend: List<Pair<String, Int>> = emptyList(),
)

// ─────────────────────────────────────────────────────────────
// UiState completo de KPIs
// ─────────────────────────────────────────────────────────────
data class KpisUiState(
    val metrics:      KpiMetrics = KpiMetrics(),
    val activePeriod: KpiPeriod  = KpiPeriod.TODAY,
    val isLoading:    Boolean    = true,
    val error:        String?    = null,
)

// ─────────────────────────────────────────────────────────────
// ViewModel de KPIs
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class KpisViewModel @Inject constructor(
    private val routeRepo: RouteRepository,
    private val stopRepo:  StopRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(KpisUiState())
    val ui: StateFlow<KpisUiState> = _ui.asStateFlow()

    private var allRoutes: List<RouteEntity> = emptyList()
    private var allStops:  List<StopEntity>  = emptyList()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            routeRepo.observeAll()
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { routes ->
                    allRoutes = routes
                    refreshStopsForRoutes(routes.map { it.uid })
                }
        }
    }

    private var stopsJob: kotlinx.coroutines.Job? = null

    private fun refreshStopsForRoutes(routeUids: List<String>) {
        stopsJob?.cancel()
        stopsJob = viewModelScope.launch {
            stopRepo.observeByRouteUids(routeUids)
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { stops ->
                    allStops = stops
                    recalculate(_ui.value.activePeriod)
                }
        }
    }

    fun setPeriod(period: KpiPeriod) {
        _ui.update { it.copy(activePeriod = period) }
        recalculate(period)
    }

    private fun recalculate(period: KpiPeriod) {
        val today = java.time.LocalDate.now()
        val fmt   = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

        // Filtrar rutas por período
        val filteredRoutes = allRoutes.filter { route ->
            val d = runCatching { java.time.LocalDate.parse(route.dateAssigned, fmt) }.getOrNull()
                ?: return@filter false
            when (period) {
                KpiPeriod.TODAY -> d == today
                KpiPeriod.WEEK  -> d >= today.minusDays(6) && d <= today
                KpiPeriod.MONTH -> d >= today.minusDays(29) && d <= today
            }
        }

        val routeUids = filteredRoutes.map { it.uid }.toSet()
        val stops     = allStops.filter { it.routeUid in routeUids }

        // Tendencia semanal (últimos 7 días) — independiente del período
        val weeklyTrend = (6 downTo 0).map { daysAgo ->
            val date      = today.minusDays(daysAgo.toLong())
            val dateStr   = date.format(fmt)
            val dayRoutes = allRoutes.filter { it.dateAssigned == dateStr }.map { it.uid }.toSet()
            val dayDone   = allStops.count { it.routeUid in dayRoutes && it.status == "done" }
            dateStr to dayDone
        }

        val total    = stops.size
        val done     = stops.count { it.status == "done" }
        val contacted = stops.count { it.visitResult == "contactado" }

        val metrics = KpiMetrics(
            totalStops      = total,
            doneStops       = done,
            pendingStops    = stops.count { it.status == "pending" },
            skippedStops    = stops.count { it.status == "skipped" },
            visitingStops   = stops.count { it.status == "visiting" },
            completionRate  = if (total > 0) done.toFloat() / total else 0f,
            contactRate     = if (total > 0) contacted.toFloat() / total else 0f,
            stopsWithGps    = stops.count { it.lat != null && it.lat != 0.0 },
            stopsWithoutGps = stops.count { it.lat == null || it.lat == 0.0 },
            gpsRate         = if (total > 0) stops.count { it.lat != null && it.lat != 0.0 }.toFloat() / total else 0f,
            totalRoutes     = filteredRoutes.size,
            activeRoutes    = filteredRoutes.count { it.status == "active" },
            doneRoutes      = filteredRoutes.count { it.status == "done" },
            resultContacted = stops.count { it.visitResult == "contactado" },
            resultNotHome   = stops.count { it.visitResult == "no_estaba" },
            resultReturn    = stops.count { it.visitResult == "volvemos" },
            resultRejected  = stops.count { it.visitResult == "rechazado" },
            weeklyTrend     = weeklyTrend,
        )

        _ui.update { it.copy(metrics = metrics, isLoading = false) }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

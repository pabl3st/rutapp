package com.pabl3st.rutapp.feature.kpis

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository
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
enum class KpiPeriod { TODAY, WEEK, MONTH, SIX_MONTHS }

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

    // Tendencia mensual (6 meses) — lista de (mesLabel, nStops)
    val monthlyTrend: List<Pair<String, Int>> = emptyList(),

    // KPIs del sector — lista de (KpiDefinition, totalValue, unitLabel)
    // totalValue suma todos los valores numéricos del período para ese KPI
    val sectorKpis: List<Triple<KpiDefinitionEntity, String, Boolean>> = emptyList(),
    // Triple: (definition, displayValue, isNumeric)
)

// ─────────────────────────────────────────────────────────────
// UiState completo de KPIs
// ─────────────────────────────────────────────────────────────
data class KpisUiState(
    val metrics:         KpiMetrics = KpiMetrics(),
    val activePeriod:    KpiPeriod  = KpiPeriod.TODAY,
    val routes:          List<RouteEntity> = emptyList(),
    val selectedRouteUid: String?   = null,
    val sectorKpis:      List<Triple<KpiDefinitionEntity, String, Boolean>> = emptyList(),
    val isLoading:       Boolean    = true,
    val error:           String?    = null,
)

// ─────────────────────────────────────────────────────────────
// ViewModel de KPIs
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class KpisViewModel @Inject constructor(
    private val routeRepo:    RouteRepository,
    private val stopRepo:     StopRepository,
    private val kpiValueDao:  KpiValueDao,
    private val profileRepo:  BusinessProfileRepository,
) : BaseViewModel() {

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

    fun setRouteFilter(routeUid: String?) {
        _ui.update { it.copy(selectedRouteUid = routeUid) }
        recalculate(_ui.value.activePeriod)
    }

    private fun recalculate(period: KpiPeriod) {
        val today = java.time.LocalDate.now()
        val fmt   = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

        val filteredRoutes = allRoutes.filter { route ->
            val d = runCatching { java.time.LocalDate.parse(route.dateAssigned, fmt) }.getOrNull()
                ?: return@filter false
            val inPeriod = when (period) {
                KpiPeriod.TODAY      -> d == today
                KpiPeriod.WEEK       -> d >= today.minusDays(6) && d <= today
                KpiPeriod.MONTH      -> d >= today.minusDays(29) && d <= today
                KpiPeriod.SIX_MONTHS -> d >= today.minusDays(179) && d <= today
            }
            val inRoute = _ui.value.selectedRouteUid?.let { it == route.uid } ?: true
            inPeriod && inRoute
        }

        val routeUids = filteredRoutes.map { it.uid }.toSet()
        val stops     = allStops.filter { it.routeUid in routeUids }

        // Tendencia semanal (7 días)
        val weeklyTrend = (6 downTo 0).map { daysAgo ->
            val date      = today.minusDays(daysAgo.toLong())
            val dateStr   = date.format(fmt)
            val dayRoutes = allRoutes.filter { it.dateAssigned == dateStr }.map { it.uid }.toSet()
            val dayDone   = allStops.count { it.routeUid in dayRoutes && it.status == "done" }
            dateStr to dayDone
        }

        // Tendencia mensual (6 meses)
        val monthlyTrend = (5 downTo 0).map { monthsAgo ->
            val month     = today.minusMonths(monthsAgo.toLong())
            val monthRoutes = allRoutes.filter {
                val d = runCatching { java.time.LocalDate.parse(it.dateAssigned, fmt) }.getOrNull()
                d?.year == month.year && d.monthValue == month.monthValue
            }.map { it.uid }.toSet()
            val monthDone = allStops.count { it.routeUid in monthRoutes && it.status == "done" }
            val label = month.format(java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale("es")))
                .replaceFirstChar { it.uppercase() }
            label to monthDone
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
            monthlyTrend    = monthlyTrend,
        )

        // ── KPIs del sector ───────────────────────────────────────
        _ui.update { it.copy(
            metrics  = metrics,
            routes   = allRoutes.distinctBy { it.uid },
            isLoading = false,
        ) }
        // buildSectorKpis es suspend — lanzar en coroutine separada
        viewModelScope.launch {
            val sectorKpis = buildSectorKpis(stops.map { it.uid })
            _ui.update { it.copy(sectorKpis = sectorKpis) }
        }
    }

    private suspend fun buildSectorKpis(
        stopUids: List<String>
    ): List<Triple<KpiDefinitionEntity, String, Boolean>> {
        if (stopUids.isEmpty()) return emptyList()
        val profile  = profileRepo.getOrCreateProfile()
        val kpiDefs  = profileRepo.getVisibleKpisForSector(profile.sector)
            .filter { it.type == "number" || it.type == "boolean" }
            .filter { it.id !in setOf("common_resultado", "common_duracion") }

        if (kpiDefs.isEmpty()) return emptyList()

        return kpiDefs.mapNotNull { def ->
            val values = stopUids.flatMap { uid ->
                kpiValueDao.getByStop(uid).filter { it.kpiId == def.id }
            }
            if (values.isEmpty()) return@mapNotNull null
            val display = when (def.type) {
                "boolean" -> {
                    val trueCount = values.count { it.valueText == "true" }
                    "$trueCount/${values.size}"
                }
                "number"  -> {
                    val sum = values.sumOf { it.valueText.toDoubleOrNull() ?: 0.0 }
                    val unit = def.unit?.let { " $it" } ?: ""
                    if (sum == sum.toLong().toDouble()) "${sum.toLong()}$unit"
                    else "${"%.1f".format(sum)}$unit"
                }
                else -> values.lastOrNull()?.valueText ?: ""
            }
            Triple(def, display, def.type == "number")
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(
            isLoading = false,
            error     = t.message ?: "Error inesperado",
        )}
    }

}

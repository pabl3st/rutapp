package com.pabl3st.rutapp.feature.kpis

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.network.StatsMonthAgent
import com.pabl3st.rutapp.data.network.StatsMonthKpi
import com.pabl3st.rutapp.data.network.StatsMonthVisits
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.session.SessionManager
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

    // Plus/PlusLL (solo sector telco)
    val plusPdvs:    Int = 0,   // PDVs que cumplen Plus: activaciones>=5 OR primerBono>=50€
    val plusLlPdvs:  Int = 0,   // PDVs Plus marcados manualmente como "Plus LL" (telco_plus=true)
    val totalTelco:  Int = 0,   // PDVs con algún KPI telco registrado

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
    val routeOutOfPeriod: Boolean   = false,  // ruta seleccionada no tiene visitas en el período
    val sectorKpis:      List<Triple<KpiDefinitionEntity, String, Boolean>> = emptyList(),
    val isLoading:       Boolean    = true,
    val error:           String?    = null,
    // Datos del servidor (solo manager/admin/owner)
    val serverStats:     StatsMonthVisits?    = null,
    val serverKpis:      List<StatsMonthKpi>  = emptyList(),
    val serverAgents:    List<StatsMonthAgent> = emptyList(),
    val isManager:       Boolean              = false,
    val isLoadingServer: Boolean              = false,
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
    private val api:          RutasApiService,
    private val session:      SessionManager,
) : BaseViewModel() {

    private val _ui = MutableStateFlow(KpisUiState())
    val ui: StateFlow<KpisUiState> = _ui.asStateFlow()

    private var allRoutes: List<RouteEntity> = emptyList()
    private var allStops:  List<StopEntity>  = emptyList()

    init {
        val role = session.userRole
        val isManager = role in listOf("manager", "admin", "owner", "god")
        _ui.update { it.copy(isManager = isManager) }
        observeData()
        if (isManager) loadServerStats()
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

        // Helper: una ruta está activa en una fecha si dateAssigned == date
        // O si scheduledDates (CSV "2026-05-06,2026-05-14,2026-05-18") contiene esa fecha
        fun routeDatesSet(route: com.pabl3st.rutapp.data.local.entity.RouteEntity): Set<String> {
            val dates = mutableSetOf<String>()
            route.dateAssigned.takeIf { it.isNotBlank() && it != "1970-01-01" }?.let { dates.add(it) }
            route.scheduledDates?.forEach { dates.add(it) }
            return dates
        }

        fun isRouteActiveOn(route: com.pabl3st.rutapp.data.local.entity.RouteEntity, date: java.time.LocalDate): Boolean {
            val dateStr = date.format(fmt)
            return routeDatesSet(route).contains(dateStr)
        }

        fun isRouteActiveInRange(route: com.pabl3st.rutapp.data.local.entity.RouteEntity,
                                  from: java.time.LocalDate, to: java.time.LocalDate): Boolean {
            return routeDatesSet(route).any { dateStr ->
                val d = runCatching { java.time.LocalDate.parse(dateStr, fmt) }.getOrNull()
                d != null && d >= from && d <= to
            }
        }

        val filteredRoutes = allRoutes.filter { route ->
            val inPeriod = when (period) {
                KpiPeriod.TODAY      -> isRouteActiveOn(route, today)
                KpiPeriod.WEEK       -> isRouteActiveInRange(route, today.minusDays(6), today)
                KpiPeriod.MONTH      -> isRouteActiveInRange(route, today.minusDays(29), today)
                KpiPeriod.SIX_MONTHS -> isRouteActiveInRange(route, today.minusDays(179), today)
            }
            val inRoute = _ui.value.selectedRouteUid?.let { it == route.uid } ?: true
            inPeriod && inRoute
        }

        val routeUids = filteredRoutes.map { it.uid }.toSet()
        // Detectar si la ruta seleccionada existe pero no tiene visitas en este período
        val selectedUid = _ui.value.selectedRouteUid
        val routeOutOfPeriod = selectedUid != null &&
            allRoutes.any { it.uid == selectedUid } &&
            selectedUid !in routeUids
        val stops = allStops.filter { it.routeUid in routeUids }

        // Tendencia semanal (7 días)
        val weeklyTrend = (6 downTo 0).map { daysAgo ->
            val date      = today.minusDays(daysAgo.toLong())
            val dateStr   = date.format(fmt)
            val dayRoutes = allRoutes.filter { isRouteActiveOn(it, date) }.map { it.uid }.toSet()
            val dayDone   = allStops.count { it.routeUid in dayRoutes && it.status == "done" }
            dateStr to dayDone
        }

        // Tendencia mensual (6 meses)
        val monthlyTrend = (5 downTo 0).map { monthsAgo ->
            val month     = today.minusMonths(monthsAgo.toLong())
            val monthStart = month.withDayOfMonth(1)
            val monthEnd   = month.withDayOfMonth(month.lengthOfMonth())
            val monthRoutes = allRoutes.filter {
                isRouteActiveInRange(it, monthStart, monthEnd)
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
            routeOutOfPeriod = routeOutOfPeriod,
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
        // buildSectorKpis + Plus lógica — en coroutine separada (suspend)
        viewModelScope.launch {
            val stopUids = stops.map { it.uid }
            val sectorKpis = buildSectorKpis(stopUids)
            val (plusCount, plusLlCount, totalTelco) = calcPlusMetrics(stopUids)
            _ui.update { it.copy(
                sectorKpis = sectorKpis,
                metrics    = _ui.value.metrics.copy(
                    plusPdvs   = plusCount,
                    plusLlPdvs = plusLlCount,
                    totalTelco = totalTelco,
                ),
            ) }
        }
    }

    /**
     * Calcula métricas Plus/PlusLL para sector telco.
     * Plus: activaciones >= 5 OR primer_bono >= 50€ (lógica OR, igual que PWA)
     * PlusLL: telco_plus boolean = true
     * Returns Triple(plusCount, plusLlCount, totalTelcoStops)
     */
    private suspend fun calcPlusMetrics(stopUids: List<String>): Triple<Int, Int, Int> {
        if (stopUids.isEmpty()) return Triple(0, 0, 0)
        val profile = profileRepo.getOrCreateProfile()
        if (profile.sector != "telco") return Triple(0, 0, 0)

        val allValues = kpiValueDao.getByStops(stopUids)
        val byStop = allValues.groupBy { it.stopUid }

        var plusCount  = 0
        var plusLlCount = 0
        var telcoCount = 0

        for ((_, values) in byStop) {
            val kpiMap = values.associateBy { it.kpiId }
            val activaciones = kpiMap["telco_activaciones"]?.valueText?.toDoubleOrNull() ?: 0.0
            val primerBono   = kpiMap["telco_primer_bono"]?.valueText?.toDoubleOrNull() ?: 0.0
            val isPlus       = kpiMap["telco_plus"]?.valueText == "true"

            // Solo contar como telco si tiene algún KPI registrado
            if (kpiMap.isNotEmpty()) {
                telcoCount++
                // OR logic: activaciones>=5 OR primerBono>=50
                if (activaciones >= 5.0 || primerBono >= 50.0) plusCount++
                if (isPlus) plusLlCount++
            }
        }
        return Triple(plusCount, plusLlCount, telcoCount)
    }

    private suspend fun buildSectorKpis(
        stopUids: List<String>
    ): List<Triple<KpiDefinitionEntity, String, Boolean>> {
        if (stopUids.isEmpty()) return emptyList()
        val profile = profileRepo.getOrCreateProfile()
        val kpiDefs = profileRepo.getVisibleKpisForSector(profile.sector)
            .filter { it.type == "number" || it.type == "boolean" }
            .filter { it.id !in setOf("common_resultado", "common_duracion") }

        if (kpiDefs.isEmpty()) return emptyList()

        // Carga todos los kpi_values del período en una sola query (eficiente)
        val allValues = kpiValueDao.getByStops(stopUids)
        val byKpi = allValues.groupBy { it.kpiId }

        return kpiDefs.mapNotNull { def ->
            val values = byKpi[def.id] ?: return@mapNotNull null
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

    // ── Stats del servidor (manager/owner) ───────────────────
    private fun loadServerStats(month: String = java.time.YearMonth.now().toString()) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoadingServer = true) }
            val token = session.token ?: run {
                _ui.update { it.copy(isLoadingServer = false) }
                return@launch
            }
            runCatching {
                val resp = api.statsMonth(token = token, month = month)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    val body = resp.body()!!
                    _ui.update { it.copy(
                        serverStats     = body.visits,
                        serverKpis      = body.kpiAggregates,
                        serverAgents    = body.agents,
                        isLoadingServer = false,
                    ) }
                } else {
                    _ui.update { it.copy(isLoadingServer = false) }
                }
            }.onFailure {
                _ui.update { it.copy(isLoadingServer = false) }
            }
        }
    }

    fun refreshServerStats() {
        if (_ui.value.isManager) loadServerStats()
    }

    fun clearError() = _ui.update { it.copy(error = null) }
    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(
            isLoading = false,
            error     = t.message ?: "Error inesperado",
        )}
    }

}

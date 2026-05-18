@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.kpis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pabl3st.rutapp.data.network.StatsMonthAgent
import com.pabl3st.rutapp.data.network.StatsMonthKpi
import com.pabl3st.rutapp.data.network.StatsMonthVisits
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────
// KpisScreen — Dashboard de métricas del agente comercial
// ─────────────────────────────────────────────────────────────
@Composable
fun KpisScreen(
    onNavigateToBiblioteca: () -> Unit = {},
    vm: KpisViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(


        modifier = Modifier.semantics { testTag = "kpis-screen" },
        topBar = {
            TopAppBar(
                title = { Text("KPIs") },
                actions = {
                    IconButton(onClick = onNavigateToBiblioteca) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Biblioteca de paradas")
                    }
                },
            )
        }
    ) { padding ->
        if (ui.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {

            // ── Selector de período ───────────────────────────
            item {
                PeriodSelector(
                    active   = ui.activePeriod,
                    onSelect = vm::setPeriod,
                )
            }

            // ── Filtro por ruta ───────────────────────────────
            if (ui.routes.size > 1) {
                item {
                    RouteFilterChips(
                        routes          = ui.routes,
                        selectedRouteUid = ui.selectedRouteUid,
                        onSelect        = vm::setRouteFilter,
                    )
                }
            }

            // ── Métricas principales (2x2 grid) ───────────────
            item {
                Text(
                    "Visitas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Spacing.sm))
                // Aviso cuando la ruta seleccionada existe pero no tiene visitas en este período
                if (ui.routeOutOfPeriod) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                        color    = MaterialTheme.colorScheme.surfaceVariant,
                        shape    = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Icon(Icons.Default.Info, null,
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Esta ruta no tiene visitas en el período seleccionado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value    = "${ui.metrics.doneStops}/${ui.metrics.totalStops}",
                        label    = "Completadas",
                        icon     = Icons.Default.CheckCircle,
                        color    = MaterialTheme.colorScheme.secondary,
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value    = "${(ui.metrics.completionRate * 100).toInt()}%",
                        label    = "Ratio visita",
                        icon     = Icons.AutoMirrored.Filled.TrendingUp,
                        color    = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value    = "${(ui.metrics.contactRate * 100).toInt()}%",
                        label    = "Ratio contacto",
                        icon     = Icons.Default.Person,
                        color    = MaterialTheme.colorScheme.tertiary,
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value    = "${ui.metrics.skippedStops}",
                        label    = "Omitidos",
                        icon     = Icons.Default.Cancel,
                        color    = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // ── Tendencia semanal ─────────────────────────────
            item {
                SectionTitle("Visitas últimos 7 días")
                Spacer(Modifier.height(Spacing.sm))
                WeeklyTrendChart(
                    data     = ui.metrics.weeklyTrend,
                    barColor = MaterialTheme.colorScheme.primary,
                )
            }

            // ── Tendencia 6 meses ─────────────────────────────
            if (ui.activePeriod == KpiPeriod.SIX_MONTHS || ui.activePeriod == KpiPeriod.MONTH) {
                item {
                    SectionTitle("Tendencia mensual (6 meses)")
                    Spacer(Modifier.height(Spacing.sm))
                    WeeklyTrendChart(
                        data     = ui.metrics.monthlyTrend,
                        barColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            // ── Resultados de visita ───────────────────────────
            if (ui.metrics.totalStops > 0) {
                item {
                    SectionTitle("Resultado de visitas")
                    Spacer(Modifier.height(Spacing.sm))
                    VisitResultBreakdown(metrics = ui.metrics)
                }
            }

            // ── GPS coverage ──────────────────────────────────
            item {
                SectionTitle("Cobertura GPS")
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value    = "${ui.metrics.stopsWithGps}",
                        label    = "Con GPS",
                        icon     = Icons.Default.LocationOn,
                        color    = MaterialTheme.colorScheme.secondary,
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value    = "${ui.metrics.stopsWithoutGps}",
                        label    = "Sin GPS",
                        icon     = Icons.Default.LocationOff,
                        color    = if (ui.metrics.stopsWithoutGps > 0)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (ui.metrics.stopsWithoutGps > 0) {
                    Spacer(Modifier.height(Spacing.sm))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier          = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                "${ui.metrics.stopsWithoutGps} stops sin coordenadas GPS — no aparecen en el mapa",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            // ── Rutas del período ─────────────────────────────
            item {
                SectionTitle("Rutas")
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value    = "${ui.metrics.totalRoutes}",
                        label    = "Total",
                        icon     = Icons.Default.Route,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value    = "${ui.metrics.doneRoutes}",
                        label    = "Completadas",
                        icon     = Icons.Default.CheckCircle,
                        color    = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // ── KPIs del sector activo ────────────────────────
            if (ui.sectorKpis.isNotEmpty()) {
                item {
                    SectionTitle("KPIs del sector")
                    Spacer(Modifier.height(Spacing.sm))
                    SectorKpisGrid(sectorKpis = ui.sectorKpis)
                }
            }

            // ── Equipo este mes (solo manager/owner) ──────────
            if (ui.isManager) {
                item {
                    Spacer(Modifier.height(Spacing.sm))
                    SectionTitle("Equipo este mes")
                }
                if (ui.isLoadingServer) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                } else {
                    ui.serverStats?.let { sv -> item { TeamSummaryCard(sv) } }
                    if (ui.serverAgents.isNotEmpty()) {
                        item {
                            Text(
                                "Por agente",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                        items(ui.serverAgents) { agent -> AgentRow(agent) }
                    }
                    if (ui.serverKpis.isNotEmpty()) {
                        item {
                            Text(
                                "KPIs del equipo",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                        items(ui.serverKpis) { kpi -> ServerKpiRow(kpi) }
                    }
                }
            }

            // Padding inferior
            item { Spacer(Modifier.height(Spacing.xl)) }
        }
    }
}

// ── Selector de período ───────────────────────────────────────
@Composable
private fun PeriodSelector(
    active:   KpiPeriod,
    onSelect: (KpiPeriod) -> Unit,
) {
    val periods = listOf(
        KpiPeriod.TODAY      to "Hoy",
        KpiPeriod.WEEK       to "7 días",
        KpiPeriod.MONTH      to "30 días",
        KpiPeriod.SIX_MONTHS to "6 meses",
    )
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        periods.forEach { (period, label) ->
            FilterChip(
                selected = active == period,
                onClick  = { onSelect(period) },
                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Tarjeta de métrica individual ────────────────────────────
@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    value:    String,
    label:    String,
    icon:     ImageVector,
    color:    Color,
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(),
    ) {
        Column(
            modifier            = Modifier.padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text      = value,
                style     = MaterialTheme.typography.headlineSmall,
                color     = color,
                textAlign = TextAlign.Center,
            )
            Text(
                text      = label,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Gráfico de barras de tendencia semanal ────────────────────
@Composable
private fun WeeklyTrendChart(
    data:     List<Pair<String, Int>>,
    barColor: Color,
) {
    val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale("es"))
    val maxVal = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                val barWidth  = size.width / (data.size * 1.5f)
                val spacing   = size.width / data.size
                val chartH    = size.height

                data.forEachIndexed { index, (_, count) ->
                    val x        = spacing * index + spacing / 2f
                    val barH     = (count.toFloat() / maxVal) * chartH
                    val topY     = chartH - barH
                    val alpha    = if (count == 0) 0.2f else 1f

                    drawRoundRect(
                        color        = barColor.copy(alpha = alpha),
                        topLeft      = Offset(x - barWidth / 2, topY),
                        size         = androidx.compose.ui.geometry.Size(barWidth, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                data.forEach { (dateStr, count) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (count > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Text(
                            text  = runCatching {
                                LocalDate.parse(dateStr).format(dayFmt)
                                    .replaceFirstChar { it.uppercase() }
                            }.getOrDefault("—"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Desglose de resultados de visita ─────────────────────────
@Composable
private fun VisitResultBreakdown(metrics: KpiMetrics) {
    val total = metrics.totalStops.coerceAtLeast(1)
    val items = listOf(
        Triple("Contactado",  metrics.resultContacted, MaterialTheme.colorScheme.secondary),
        Triple("No estaba",   metrics.resultNotHome,   MaterialTheme.colorScheme.tertiary),
        Triple("Volvemos",    metrics.resultReturn,    MaterialTheme.colorScheme.primary),
        Triple("Rechazado",   metrics.resultRejected,  MaterialTheme.colorScheme.error),
    )
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            items.forEach { (label, count, color) ->
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(90.dp),
                        color    = MaterialTheme.colorScheme.onSurface,
                    )
                    LinearProgressIndicator(
                        progress        = { count.toFloat() / total },
                        modifier        = Modifier.weight(1f).height(6.dp),
                        color           = color,
                        trackColor      = color.copy(alpha = 0.15f),
                        strokeCap       = StrokeCap.Round,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "$count",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = color,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}


@Composable
private fun SectorKpisGrid(
    sectorKpis: List<Triple<KpiDefinitionEntity, String, Boolean>>,
) {
    val rows = sectorKpis.chunked(2)
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            rows.forEachIndexed { rowIdx, row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    row.forEach { (def, value, isNumeric) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                        ) {
                            Column(
                                modifier            = Modifier.padding(Spacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text      = value,
                                    style     = MaterialTheme.typography.headlineSmall,
                                    color     = if (isNumeric) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text      = def.label,
                                    style     = MaterialTheme.typography.labelSmall,
                                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines  = 2,
                                    overflow  = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                if (rowIdx < rows.size - 1) Spacer(Modifier.height(Spacing.sm))
            }
        }
    }
}

// ── Título de sección ─────────────────────────────────────────
@Composable
private fun TeamSummaryCard(sv: StatsMonthVisits) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                TeamStatCell("Visitas",    sv.totalStops.toString(),    Modifier.weight(1f))
                TeamStatCell("Completadas", sv.doneStops.toString(),    Modifier.weight(1f))
                TeamStatCell("Agentes",    sv.activeAgents.toString(),  Modifier.weight(1f))
            }
            if (sv.totalStops > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    TeamStatCell(
                        "Ratio visita",
                        "${"%.0f".format(sv.doneStops * 100f / sv.totalStops)}%",
                        Modifier.weight(1f),
                    )
                    TeamStatCell(
                        "Contactados",
                        "${"%.0f".format(sv.contacted * 100f / sv.totalStops)}%",
                        Modifier.weight(1f),
                    )
                    TeamStatCell("Rutas", sv.totalRoutes.toString(), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TeamStatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AgentRow(agent: StatsMonthAgent) {
    val ratio = if (agent.totalStops > 0)
        "${"%.0f".format(agent.doneStops * 100f / agent.totalStops)}%" else "—"
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(agent.name, style = MaterialTheme.typography.bodyMedium)
                Text("@${agent.username}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("${agent.doneStops}/${agent.totalStops}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text(ratio, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ServerKpiRow(kpi: StatsMonthKpi) {
    val display = when (kpi.type) {
        "boolean" -> "${kpi.trueCount}/${kpi.countEntries}"
        "number"  -> {
            val v = kpi.totalValue
            val formatted = if (v == v.toLong().toDouble()) "${v.toLong()}" else "%.1f".format(v)
            kpi.unit?.let { "$formatted $it" } ?: formatted
        }
        else -> kpi.countEntries.toString()
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(kpi.label, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f))
            Text(display, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

// ── Filtro por ruta ───────────────────────────────────────────
@Composable
private fun RouteFilterChips(
    routes: List<com.pabl3st.rutapp.data.local.entity.RouteEntity>,
    selectedRouteUid: String?,
    onSelect: (String?) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        item {
            FilterChip(
                selected = selectedRouteUid == null,
                onClick  = { onSelect(null) },
                label    = { Text("Todas", style = MaterialTheme.typography.labelSmall) },
            )
        }
        items(routes) { route ->
            FilterChip(
                selected = selectedRouteUid == route.uid,
                onClick  = { onSelect(if (selectedRouteUid == route.uid) null else route.uid) },
                label    = {
                    Text(
                        route.name,
                        style   = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 100.dp),
                    )
                },
            )
        }
    }
}

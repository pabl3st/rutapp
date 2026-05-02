@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.kpis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    vm: KpisViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KPIs") },
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

            // ── Métricas principales (2x2 grid) ───────────────
            item {
                Text(
                    "Visitas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Spacing.sm))
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
                        icon     = Icons.Default.TrendingUp,
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
        KpiPeriod.TODAY to "Hoy",
        KpiPeriod.WEEK  to "7 días",
        KpiPeriod.MONTH to "30 días",
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

// ── Título de sección ─────────────────────────────────────────
@Composable
private fun SectionTitle(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

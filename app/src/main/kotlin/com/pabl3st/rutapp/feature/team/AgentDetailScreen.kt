@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.network.AgentRecentVisitDto
import com.pabl3st.rutapp.data.network.AgentTodayRouteDto

/**
 * Detalle de un agente: jornada activa, rutas de hoy, visitas recientes, KPIs del mes.
 * Visible para: manager (sus agentes directos), admin/owner/god.
 */
@Composable
fun AgentDetailScreen(
    onBack:         () -> Unit,
    onNavigateToMap: ((Double, Double) -> Unit)? = null,
    vm: AgentDetailViewModel = hiltViewModel(),
) {
    val ui   by vm.ui.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(ui.error) {
        ui.error?.let { snack.showSnackbar(it); vm.clearError() }
    }

    val detail  = ui.detail
    val agent   = detail?.agent
    val jornada = detail?.jornada

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(agent?.name ?: "Agente") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh, enabled = !ui.isRefreshing) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = ui.isRefreshing,
            onRefresh    = vm::refresh,
            modifier     = Modifier.padding(padding),
        ) {
            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                detail == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se pudo cargar la información del agente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    // ── Cabecera del agente ───────────────────
                    AgentHeader(
                        name     = agent?.name ?: "",
                        username = agent?.username ?: "",
                        role     = agent?.role ?: "agent",
                        email    = agent?.email ?: "",
                    )

                    // ── Jornada de hoy ────────────────────────
                    JornadaCard(jornada = jornada, onNavigateToMap = onNavigateToMap)

                    // ── KPIs del mes ──────────────────────────
                    detail.monthKpis?.let { kpis ->
                        SectionTitle("KPIs de ${java.time.YearMonth.now().let {
                            it.month.getDisplayName(java.time.format.TextStyle.FULL,
                                java.util.Locale("es")) + " " + it.year }}")
                        MonthKpisCard(kpis = kpis)
                    }

                    // ── Rutas de hoy ──────────────────────────
                    if (detail.todayRoutes.isNotEmpty()) {
                        SectionTitle("Rutas de hoy")
                        detail.todayRoutes.forEach { route ->
                            TodayRouteCard(route = route)
                        }
                    } else {
                        SectionTitle("Rutas de hoy")
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(Modifier.padding(Spacing.md).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.EventBusy, null, Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Sin rutas asignadas hoy",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // ── Últimas visitas ───────────────────────
                    if (detail.recentVisits.isNotEmpty()) {
                        SectionTitle("Últimas visitas")
                        detail.recentVisits.forEach { visit ->
                            RecentVisitCard(visit = visit)
                        }
                    }

                    Spacer(Modifier.height(Spacing.xl))
                }
            }
        }
    }
}

@Composable
private fun AgentHeader(name: String, username: String, role: String, email: String) {
    Card {
        Row(Modifier.padding(Spacing.md).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("@$username · $role",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (email.isNotBlank()) {
                    Text(email, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun JornadaCard(
    jornada: Map<String, Any?>?,
    onNavigateToMap: ((Double, Double) -> Unit)?,
) {
    val state    = jornada?.get("state") as? String
    val elapsedMs = (jornada?.get("elapsed_ms") as? Double)?.toLong() ?: 0L
    val distanceKm = jornada?.get("distance_km") as? Double ?: 0.0
    val lat = jornada?.get("last_lat") as? Double
    val lng = jornada?.get("last_lng") as? Double

    SectionTitle("Jornada de hoy")
    Card {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                val (label, color) = when (state) {
                    "running" -> "En ruta ahora" to MaterialTheme.colorScheme.primary
                    "paused"  -> "Pausada" to MaterialTheme.colorScheme.tertiary
                    "done"    -> "Finalizada" to MaterialTheme.colorScheme.secondary
                    else      -> "Sin jornada hoy" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state == "running") {
                        Icon(Icons.Default.FiberManualRecord, null, Modifier.size(10.dp), tint = color)
                    }
                    Text(label, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium, color = color)
                }
                if (lat != null && lng != null && onNavigateToMap != null) {
                    OutlinedButton(onClick = { onNavigateToMap(lat, lng) },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Icon(Icons.Default.Map, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ver en mapa", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (state != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                    val hours   = elapsedMs / 3_600_000
                    val minutes = (elapsedMs % 3_600_000) / 60_000
                    StatItem(Icons.Default.Timer,   "%dh %02dm".format(hours, minutes), "Tiempo activo")
                    StatItem(Icons.Default.Route,   "%.1f km".format(distanceKm), "Distancia")
                    if (lat != null) {
                        StatItem(Icons.Default.LocationOn, "GPS activo", "Última posición")
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthKpisCard(kpis: com.pabl3st.rutapp.data.network.AgentMonthKpisDto) {
    Card {
        Row(Modifier.padding(Spacing.md).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            KpiItem("${kpis.done}", "Visitas", "${(kpis.completionRate * 100).toInt()}%")
            KpiItem("${kpis.contacted}", "Contactados", "${(kpis.contactRate * 100).toInt()}%")
            KpiItem("${kpis.notHome}", "No estaba", "")
            KpiItem("${kpis.rejected}", "Rechazados", "")
        }
    }
}

@Composable
private fun TodayRouteCard(route: AgentTodayRouteDto) {
    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(Modifier.padding(Spacing.md).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Column(Modifier.weight(1f)) {
                Text(route.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${route.doneStops}/${route.totalStops} paradas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val progress = if (route.totalStops > 0)
                route.doneStops.toFloat() / route.totalStops else 0f
            CircularProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color      = if (progress >= 1f) MaterialTheme.colorScheme.secondary
                             else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RecentVisitCard(visit: AgentRecentVisitDto) {
    val (label, color) = when (visit.visitResult) {
        "contactado" -> "Contactado" to androidx.compose.ui.graphics.Color(0xFF1D9E75)
        "no_estaba"  -> "No estaba"  to MaterialTheme.colorScheme.error
        "volvemos"   -> "Volvemos"   to MaterialTheme.colorScheme.tertiary
        "rechazado"  -> "Rechazado"  to MaterialTheme.colorScheme.error
        else         -> (visit.visitResult ?: "Sin resultado") to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Row(Modifier.padding(horizontal = Spacing.md, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(visit.name, style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                visit.routeName?.let { rn ->
                    Text(rn, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                visit.visitedAt?.take(10)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String, label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, null, Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
        }
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KpiItem(value: String, label: String, pct: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (pct.isNotBlank()) {
            Text(pct, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

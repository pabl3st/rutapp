@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.home

import com.pabl3st.rutapp.core.ui.theme.RouteStatusTokens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ────────────────────────────────────────────────────────────
// HomeScreen — Lista de rutas de hoy con progreso
// ────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    onRouteClick:      (String) -> Unit   = {},
    onStopClick:       (String) -> Unit   = {},
    onNavigateToMapa:  () -> Unit          = {},
    onNavigateToTeam:  (() -> Unit)?       = null,  // solo manager/admin/owner/god
    vm: HomeViewModel = hiltViewModel(),
) {
    val ui    by vm.ui.collectAsStateWithLifecycle()
    val today  = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("es")))
        .replaceFirstChar { it.uppercase() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },


        modifier = Modifier.semantics { testTag = "home-screen" },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Hola, ${ui.userName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            today,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    // Botón "Mi equipo" para manager/admin/owner
                    if (onNavigateToTeam != null) {
                        IconButton(onClick = onNavigateToTeam) {
                            Icon(Icons.Default.Group, contentDescription = "Mi equipo")
                        }
                    }
                    // Mapa global — solo roles con rutas asignadas
                    if (!UserRole.from(ui.userRole).isViewer && !UserRole.from(ui.userRole).isGod) {
                        IconButton(onClick = onNavigateToMapa) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = "Mapa del día",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (ui.isSyncing) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                    } else {
                        IconButton(onClick = vm::syncNow) {
                            Icon(Icons.Default.Sync, contentDescription = "Sincronizar")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            ui.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            ui.routes.isEmpty() -> EmptyRoutesMessage(
                Modifier.fillMaxSize().padding(padding)
            )

            else -> RouteListContent(
                ui           = ui,
                modifier     = Modifier.padding(padding),
                onRouteClick = onRouteClick,
            )
        }
    }
}

// ── Contenido principal: stats + jornada(s) + lista de rutas ─
@Composable
private fun RouteListContent(
    ui: HomeUiState,
    modifier: Modifier = Modifier,
    onRouteClick: (String) -> Unit,
) {
    LazyColumn(
        modifier            = modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // ── Stats del día ─────────────────────────────────────
        item {
            DayStatsRow(
                totalRoutes   = ui.routes.size,
                doneStops     = ui.doneStops,
                pendingStops  = ui.pendingStops,
            )
        }

        // ── Panel de equipo para manager ──────────────────────
        if (ui.isManager && ui.routes.isNotEmpty()) {
            item { ManagerTeamSummary(routes = ui.routes) }
        }

        // ── JornadaBar para todas las rutas de hoy operativas ──
        // La jornada se gestiona via DaySessionEntity (idle/running/paused/done)
        // independientemente del route.status — no filtrar por "active"
        val jornadaRoutes = ui.routes.filter {
            it.route.status !in listOf("cancelled", "done")
        }
        if (jornadaRoutes.isNotEmpty()) {
            items(jornadaRoutes, key = { "jornada_${it.route.uid}" }) { rwp ->
                JornadaBar(routeUid = rwp.route.uid)
            }
        }

        // ── Sección rutas ─────────────────────────────────────
        item {
            Text(
                text  = "RUTAS DE HOY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.xs),
            )
        }

        items(ui.routes, key = { it.route.uid }) { rwp ->
            RouteProgressCard(
                rwp      = rwp,
                onClick  = { onRouteClick(rwp.route.uid) },
            )
        }

        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}

// ── ManagerTeamSummary ───────────────────────────────────────
@Composable
private fun ManagerTeamSummary(routes: List<RouteWithProgress>) {
    val byAgent = routes.groupBy { it.route.userId }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                "Equipo hoy — ${byAgent.size} agente${if (byAgent.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            byAgent.forEach { (_, agentRoutes) ->
                val totalStops = agentRoutes.sumOf { it.totalStops }
                val doneStops  = agentRoutes.sumOf { it.doneStops }
                val progress   = if (totalStops > 0) doneStops.toFloat() / totalStops else 0f
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            agentRoutes.joinToString(", ") { it.route.name },
                            style    = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "$doneStops/$totalStops",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

// ── Stats del día: 3 cajas ────────────────────────────────────
@Composable
private fun DayStatsRow(
    totalRoutes:  Int,
    doneStops:    Int,
    pendingStops: Int,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        StatBox(
            value    = totalRoutes.toString(),
            label    = "Rutas hoy",
            modifier = Modifier.weight(1f),
        )
        StatBox(
            value    = doneStops.toString(),
            label    = "Visitadas",
            modifier = Modifier.weight(1f),
            color    = MaterialTheme.colorScheme.primary,
        )
        StatBox(
            value    = pendingStops.toString(),
            label    = "Pendientes",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatBox(
    value:    String,
    label:    String,
    modifier: Modifier = Modifier,
    color:    androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier,
        shape    = MaterialTheme.shapes.small,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier              = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text  = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Tarjeta de ruta con barra de progreso ─────────────────────
@Composable
private fun RouteProgressCard(
    rwp:     RouteWithProgress,
    onClick: () -> Unit,
) {
    val route       = rwp.route
    val progressAnim by animateFloatAsState(
        targetValue  = rwp.progress,
        animationSpec = tween(durationMillis = 600),
        label        = "progress_${route.uid}",
    )

    // Color según estado — tokens centralizados
    val _st = RouteStatusTokens.of(route.status)
    val statusLabel = _st.label
    val statusColor = _st.color

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {

            // ── Cabecera: icono + nombre + chip estado ────────
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Icono con fondo según estado
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (route.status == "done")
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Default.Route,
                        contentDescription = null,
                        modifier           = Modifier.size(20.dp),
                        tint               = if (route.status == "done")
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                // Nombre y fecha
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text      = route.name,
                        style     = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                    )
                    // Calcular label de fecha:
                    // si hoy está en scheduledDates → "Hoy"
                    // si dateAssigned == hoy → "Hoy"
                    // si dateAssigned == "1970-01-01" o vacío → "Sin fecha"
                    // si hoy está en scheduledDates pero dateAssigned es pasada → "Hoy (+ fechas)"
                    val todayStr = java.time.LocalDate.now().toString()
                    val dateLabel = when {
                        route.dateAssigned == todayStr -> "Hoy"
                        route.scheduledDates?.contains(todayStr) == true -> "Hoy"
                        route.dateAssigned.isBlank() || route.dateAssigned == "1970-01-01" -> "Sin fecha"
                        else -> {
                            runCatching {
                                val fmt = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                                val d = java.time.LocalDate.parse(route.dateAssigned, fmt)
                                val tomorrow = java.time.LocalDate.now().plusDays(1)
                                val yesterday = java.time.LocalDate.now().minusDays(1)
                                when (d) {
                                    java.time.LocalDate.now() -> "Hoy"
                                    tomorrow  -> "Mañana"
                                    yesterday -> "Ayer"
                                    else -> d.format(java.time.format.DateTimeFormatter
                                        .ofPattern("d MMM yyyy", java.util.Locale("es")))
                                }
                            }.getOrDefault(route.dateAssigned)
                        }
                    }
                    Text(
                        text  = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Chip de estado
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = statusColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text  = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // ── Siguiente parada pendiente ───────────────────
            rwp.nextPendingStop?.let { stop ->
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier              = Modifier.padding(bottom = 4.dp),
                ) {
                    Icon(Icons.Default.NavigateNext, null,
                        Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stop.name,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    stop.address?.takeIf { it.isNotBlank() }?.let { addr ->
                        Text("· $addr",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── Progreso ──────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Barra lineal de progreso
                LinearProgressIndicator(
                    progress       = { progressAnim },
                    modifier       = Modifier.weight(1f).height(6.dp).clip(MaterialTheme.shapes.extraSmall),
                    strokeCap      = StrokeCap.Round,
                    color          = if (route.status == "done")
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.primary,
                    trackColor     = MaterialTheme.colorScheme.surfaceVariant,
                )

                // Texto paradas
                Text(
                    text  = "${rwp.doneStops}/${rwp.totalStops}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Notas opcionales
            route.notes?.let { notes ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = notes,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Estado vacío ──────────────────────────────────────────────
@Composable
private fun EmptyRoutesMessage(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier         = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Route,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                "Sin rutas para hoy",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Las rutas asignadas aparecerán aquí",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

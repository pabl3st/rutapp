@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────
// HomeScreen — Lista de rutas de hoy con progreso
// ─────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    onRouteClick: (String) -> Unit = {},
    onStopClick:  (String) -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val ui    by vm.ui.collectAsStateWithLifecycle()
    val today = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("es")))
        .replaceFirstChar { it.uppercase() }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text  = "Hola, ${ui.userName.ifBlank { "–" }}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text  = today,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (ui.isSyncing) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp,
                        )
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
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // ── Stats globales del día ─────────────────────
                item {
                    DayStatsRow(
                        totalRoutes  = ui.totalRoutes,
                        doneStops    = ui.doneStops,
                        pendingStops = ui.pendingStops,
                    )
                }

                // ── JornadaBar compacta — solo si hay exactamente 1 ruta activa ──
                val activeRoute = ui.routes.firstOrNull { it.route.status == "active" }
                    ?: ui.routes.firstOrNull()
                if (activeRoute != null) {
                    item {
                        CompactJornadaBar(routeUid = activeRoute.route.uid)
                    }
                }

                // ── Cabecera sección ───────────────────────────
                item {
                    Text(
                        text  = "RUTAS DE HOY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
                    )
                }

                // ── Lista de rutas con progreso ────────────────
                items(ui.routes, key = { it.route.uid }) { rp ->
                    RouteProgressCard(
                        rp      = rp,
                        onClick = { onRouteClick(rp.route.uid) },
                    )
                }

                item { Spacer(Modifier.height(Spacing.lg)) }
            }
        }
    }
}

// ── 3 stats globales del día ──────────────────────────────────
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
            color    = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        StatBox(
            value    = doneStops.toString(),
            label    = "Visitadas",
            color    = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        StatBox(
            value    = pendingStops.toString(),
            label    = "Pendientes",
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatBox(
    value:    String,
    label:    String,
    color:    Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape    = MaterialTheme.shapes.small,
        color    = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = value,
                style = MaterialTheme.typography.titleLarge,
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

// ── JornadaBar compacta ───────────────────────────────────────
@Composable
private fun CompactJornadaBar(
    routeUid: String,
    vm: JornadaViewModel = hiltViewModel(),
) {
    LaunchedEffect(routeUid) { vm.init(routeUid) }
    val ui    by vm.ui.collectAsStateWithLifecycle()
    val state  = ui.session?.state ?: "idle"

    // Determinar color + label del estado
    val (stateLabel, stateColor) = when (state) {
        "running" -> "En jornada" to MaterialTheme.colorScheme.tertiary
        "paused"  -> "Pausada"    to MaterialTheme.colorScheme.secondary
        "done"    -> "Terminada"  to MaterialTheme.colorScheme.primary
        else      -> "Sin iniciar" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape          = MaterialTheme.shapes.small,
        color          = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier       = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Indicador de estado pulsante (solo cuando running)
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(stateColor),
            )

            // Tiempo + distancia
            Column(Modifier.weight(1f)) {
                Text(
                    text  = stateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state != "idle") {
                    Text(
                        text  = buildString {
                            append(vm.formatElapsed(ui.elapsedMs))
                            if (ui.distanceKm > 0.0) {
                                append("  ·  ")
                                append("%.1f km".format(ui.distanceKm))
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            // Botones de acción
            when (state) {
                "idle" -> {
                    FilledTonalButton(
                        onClick      = vm::start,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Iniciar", style = MaterialTheme.typography.labelMedium)
                    }
                }
                "running" -> {
                    OutlinedButton(
                        onClick        = vm::pause,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.Pause, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pausar", style = MaterialTheme.typography.labelMedium)
                    }
                    FilledTonalButton(
                        onClick        = vm::finish,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.Stop, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Fin", style = MaterialTheme.typography.labelMedium)
                    }
                }
                "paused" -> {
                    FilledTonalButton(
                        onClick        = vm::resume,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Continuar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ── Tarjeta de ruta con progreso ──────────────────────────────
@Composable
private fun RouteProgressCard(
    rp:      RouteProgress,
    onClick: () -> Unit,
) {
    val route = rp.route

    // Animación suave de la barra de progreso
    val animatedPct by animateFloatAsState(
        targetValue  = rp.pct,
        animationSpec = tween(durationMillis = 600),
        label        = "progress_${route.uid}",
    )

    val (statusLabel, statusColor) = when (route.status) {
        "active"    -> "Activa"    to MaterialTheme.colorScheme.primary
        "done"      -> "Hecha"     to MaterialTheme.colorScheme.tertiary
        "cancelled" -> "Cancelada" to MaterialTheme.colorScheme.error
        else        -> "Pendiente" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── Fila 1: icono + nombre + chip estado ──────────
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Icono de ruta coloreado por estado
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector        = Icons.Default.Route,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text     = route.name,
                        style    = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text  = route.dateAssigned,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // Chip de estado
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text     = statusLabel,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            // ── Fila 2: barra de progreso + porcentaje ────────
            if (rp.total > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text  = "${rp.done} de ${rp.total} visitadas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text       = "${(animatedPct * 100).toInt()}%",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }

                    // Barra de progreso personalizada con color por estado
                    val barColor = when {
                        rp.pct >= 1f -> MaterialTheme.colorScheme.tertiary
                        rp.pct > 0f  -> MaterialTheme.colorScheme.primary
                        else         -> MaterialTheme.colorScheme.outline
                    }
                    LinearProgressIndicator(
                        progress         = { animatedPct },
                        modifier         = Modifier.fillMaxWidth().height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color            = barColor,
                        trackColor       = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap        = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }

                // ── Fila 3: minicontadores por estado ─────────
                if (rp.visiting > 0 || rp.pending > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        if (rp.visiting > 0) {
                            StatusPill(
                                icon  = Icons.Default.PlayCircle,
                                label = "${rp.visiting} en visita",
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        if (rp.pending > 0) {
                            StatusPill(
                                icon  = Icons.Default.RadioButtonUnchecked,
                                label = "${rp.pending} pendientes",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                // Sin paradas aún
                Text(
                    text  = "Sin paradas asignadas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Pill inline de estado ─────────────────────────────────────
@Composable
private fun StatusPill(
    icon:  ImageVector,
    label: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, null, Modifier.size(12.dp), tint = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
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
            Icon(
                Icons.Default.Route, null,
                Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Sin rutas para hoy",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Las rutas aparecerán aquí cuando estén asignadas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

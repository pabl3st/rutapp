@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pabl3st.rutapp.core.ui.theme.Spacing
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.data.local.entity.StopEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStopClick:  (stopUid: String)   -> Unit = {},
    onRouteClick: (routeUid: String)  -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val ui              by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val today = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("es")))
        .replaceFirstChar { it.uppercase() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Hola, ${ui.userName}") },
                actions = {
                    if (ui.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(Spacing.sm))
                    } else {
                        IconButton(onClick = vm::syncNow) {
                            Icon(Icons.Default.Sync, contentDescription = "Sincronizar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = ui.isSyncing,
            onRefresh    = vm::syncNow,
            state        = pullState,
            modifier     = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Sync status ───────────────────────────────
                SyncStatusBar(
                    pending  = ui.pendingSync,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )

                // ── Fecha ─────────────────────────────────────
                Text(
                    text     = today,
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )

                // ── Header ruta del día ───────────────────────
                ui.route?.let { route ->
                    RouteHeader(
                        routeName  = route.name,
                        routeNotes = route.notes,
                        status     = route.status,
                        onClick    = { onRouteClick(route.uid) },
                        modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    )
                }

                // ── Jornada timer ─────────────────────────────
                ui.route?.let { route ->
                    JornadaBar(
                        routeUid = route.uid,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    )
                }

                // ── Barra de progreso del día ─────────────────
                if (ui.totalStops > 0) {
                    DaySummaryBar(
                        done       = ui.doneStops,
                        total      = ui.totalStops,
                        distanceKm = ui.distanceKm,
                        modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

                // ── Lista de paradas ──────────────────────────
                when {
                    ui.isLoading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }

                    ui.route == null -> EmptyDayMessage(modifier = Modifier.fillMaxSize())

                    ui.stops.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Place, null,
                                Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                "Sin paradas en esta ruta",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    else -> LazyColumn(
                        contentPadding      = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(ui.stops, key = { it.uid }) { stop ->
                            StopCard(
                                stop    = stop,
                                onClick = { onStopClick(stop.uid) },
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(ui.error) {
        ui.error?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            vm.clearError()
        }
    }
}

// ── Header de la ruta del día ─────────────────────────────────
@Composable
private fun RouteHeader(
    routeName:  String,
    routeNotes: String?,
    status:     String,
    onClick:    () -> Unit,
    modifier:   Modifier = Modifier,
) {
    val (color, icon, label) = when (status) {
        "active"    -> Triple(MaterialTheme.colorScheme.primary,          Icons.Default.PlayCircle,  "Activa")
        "done"      -> Triple(MaterialTheme.colorScheme.secondary,        Icons.Default.CheckCircle, "Completada")
        "cancelled" -> Triple(MaterialTheme.colorScheme.error,            Icons.Default.Cancel,      "Cancelada")
        else        -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Schedule,    "Pendiente")
    }
    Row(
        modifier          = modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(routeName, style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            routeNotes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Icon(icon, null, Modifier.size(14.dp), tint = color)
        Spacer(Modifier.width(Spacing.xs))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        Spacer(Modifier.width(Spacing.xs))
        Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Card de parada ────────────────────────────────────────────
@Composable
private fun StopCard(stop: StopEntity, onClick: () -> Unit) {
    val isDone     = stop.status == "done"
    val isVisiting = stop.status == "visiting"
    val hasGps     = stop.lat != null && stop.lat != 0.0

    val (statusColor, statusIcon) = when (stop.status) {
        "done"     -> MaterialTheme.colorScheme.secondary  to Icons.Default.CheckCircle
        "visiting" -> MaterialTheme.colorScheme.primary    to Icons.Default.Edit
        "skipped"  -> MaterialTheme.colorScheme.error      to Icons.Default.Cancel
        else       -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.RadioButtonUnchecked
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = when {
            isVisiting -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            isDone     -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            else       -> CardDefaults.cardColors()
        },
        onClick  = onClick,
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Número de orden
            Text(
                text  = "${stop.orderIndex + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = statusColor,
                modifier = Modifier.width(20.dp),
            )
            Spacer(Modifier.width(Spacing.sm))

            // Datos del stop
            Column(Modifier.weight(1f)) {
                stop.externalId?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    stop.name,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                stop.address?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                stop.contactName?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(Spacing.xs))
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.width(Spacing.sm))

            // Estado
            Icon(statusIcon, contentDescription = stop.status,
                Modifier.size(20.dp), tint = statusColor)
        }
    }
}

// ── Barra de progreso ─────────────────────────────────────────
@Composable
private fun DaySummaryBar(
    done: Int, total: Int, distanceKm: Double, modifier: Modifier = Modifier,
) {
    val progress = if (total > 0) done.toFloat() / total else 0f
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$done/$total paradas", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (distanceKm > 0.0) {
                Text("%.1f km".format(distanceKm), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        LinearProgressIndicator(
            progress   = { progress },
            modifier   = Modifier.fillMaxWidth(),
            color      = when {
                progress >= 1f -> MaterialTheme.colorScheme.secondary
                progress > 0f  -> MaterialTheme.colorScheme.primary
                else           -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap  = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

// ── Sync status ───────────────────────────────────────────────
@Composable
private fun SyncStatusBar(pending: Int, modifier: Modifier = Modifier) {
    val color = if (pending > 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (pending > 0) Icons.Default.CloudOff else Icons.Default.CloudDone,
            contentDescription = null, tint = color, modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            if (pending > 0) "$pending cambios pendientes" else "Sincronizado",
            style = MaterialTheme.typography.labelSmall, color = color,
        )
    }
}

// ── Sin ruta asignada ─────────────────────────────────────────
@Composable
private fun EmptyDayMessage(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.EventAvailable, null, Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            Spacer(Modifier.height(Spacing.lg))
            Text("Sin ruta para hoy", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.xs))
            Text("Toca sincronizar para actualizar", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

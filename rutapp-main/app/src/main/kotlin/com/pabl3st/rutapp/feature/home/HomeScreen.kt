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
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRouteClick: (String) -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val ui              by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val today  = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("es")))
        .replaceFirstChar { it.uppercase() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Hola, ${ui.userName}") },
                actions = {
                    if (ui.isSyncing) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
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

            // ── Estado de sincronización ──────────────────────
            SyncStatusBar(
                pending  = ui.pendingSync,
                lastSync = ui.lastSync,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = 4.dp),
            )

            // ── Fecha del día ─────────────────────────────────
            Text(
                text     = today,
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 4.dp),
            )

            // ── Barra de jornada — visible cuando hay ruta activa hoy ─
            if (ui.routes.size == 1) {
                JornadaBar(
                    routeUid = ui.routes.first().uid,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 4.dp),
                )
            }

            // ── Contenido ─────────────────────────────────────
            when {
                ui.isLoading -> LoadingContent()
                ui.routes.isEmpty() -> EmptyRoutesMessage(Modifier.fillMaxSize())
                else -> RoutesList(routes = ui.routes, onRouteClick = onRouteClick)
            }
        } // Column
        } // PullToRefreshBox
    }

    // ── Error snackbar ──────────────────────────────────────
    LaunchedEffect(ui.error) {
        ui.error?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            vm.clearError()
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RoutesList(routes: List<RouteEntity>, onRouteClick: (String) -> Unit) {
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = Spacing.lg, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(routes, key = { it.uid }) { route ->
            RouteCard(route = route, onClick = { onRouteClick(route.uid) })
        }
    }
}


@Composable
private fun DaySummaryBar(
    done:       Int,
    total:      Int,
    distanceKm: Double,
    modifier:   Modifier = Modifier,
) {
    val progress = if (total > 0) done.toFloat() / total else 0f
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text  = "$done/$total visitas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (distanceKm > 0.0) {
                Text(
                    text  = "%.1f km".format(distanceKm),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        LinearProgressIndicator(
            progress        = { progress },
            modifier        = Modifier.fillMaxWidth(),
            color           = when {
                progress >= 1f -> MaterialTheme.colorScheme.secondary
                progress > 0f  -> MaterialTheme.colorScheme.primary
                else           -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            trackColor      = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap       = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun SyncStatusBar(
    pending: Int,
    lastSync: String,
    modifier: Modifier = Modifier,
) {
    val color = if (pending > 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector      = if (pending > 0) Icons.Default.CloudOff else Icons.Default.CloudDone,
            contentDescription = null,
            tint             = color,
            modifier         = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text  = if (pending > 0) "$pending cambios pendientes" else "Sincronizado",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun RouteCard(route: RouteEntity, onClick: () -> Unit) {
    val statusColor = when (route.status) {
        "active"    -> MaterialTheme.colorScheme.primary
        "done"      -> MaterialTheme.colorScheme.tertiary
        "cancelled" -> MaterialTheme.colorScheme.error
        else        -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier            = Modifier.padding(Spacing.lg),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(route.name, style = MaterialTheme.typography.titleSmall)
                route.notes?.let { notes ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = notes,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            StatusChip(status = route.status)
        }
    }
}


@Composable
private fun StatusChip(status: String) {
    val (color, icon, label) = when (status) {
        "active"    -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.PlayCircle,
            "Activa",
        )
        "done"      -> Triple(
            MaterialTheme.colorScheme.secondary,
            Icons.Default.CheckCircle,
            "Completada",
        )
        "cancelled" -> Triple(
            MaterialTheme.colorScheme.error,
            Icons.Default.Cancel,
            "Cancelada",
        )
        else        -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Schedule,
            "Pendiente",
        )
    }
    SuggestionChip(
        onClick = {},
        label   = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon    = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors  = SuggestionChipDefaults.suggestionChipColors(
            labelColor         = color,
            iconContentColor   = color,
        ),
    )
}
@Composable
private fun EmptyRoutesMessage(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.Route,
                contentDescription = null,
                modifier           = Modifier.size(64.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text  = "Sin rutas para hoy",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text  = "Toca sincronizar para actualizar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}


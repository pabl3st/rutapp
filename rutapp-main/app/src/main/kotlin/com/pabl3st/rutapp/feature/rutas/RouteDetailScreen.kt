@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.rutas

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

@Composable
fun RouteDetailScreen(
    routeUid: String,
    onBack: () -> Unit,
    onNavigateToMap: (String) -> Unit = {},
    onStopClick: (String) -> Unit = {},
    onAddStop: (String) -> Unit = {},
    vm: RouteDetailViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.error) {
        ui.error?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.route?.name ?: "Ruta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToMap(routeUid) }) {
                        Icon(Icons.Default.Map, contentDescription = "Ver en mapa")
                    }
                    ui.route?.let { route ->
                        StatusChip(
                            status   = route.status,
                            modifier = Modifier.padding(end = Spacing.sm),
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddStop(routeUid) }) {
                Icon(Icons.Default.AddLocation, contentDescription = "Añadir parada")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            ui.stops.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Place, null, Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(Spacing.md))
                    Text("Sin paradas", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.xs))
                    Text("Pulsa + para añadir la primera",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
            else -> LazyColumn(
                contentPadding      = PaddingValues(horizontal = Spacing.lg, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier            = Modifier.padding(padding),
            ) {
                items(ui.stops, key = { it.uid }) { stop ->
                    StopCard(
                        stop          = stop,
                        onMarkVisited = { vm.markStopVisited(stop.uid) },
                        onOpenVisita  = { onStopClick(stop.uid) },
                    )
                }
            }
        }
    }
}


@Composable
private fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (color, icon, label) = when (status) {
        "active"    -> Triple(MaterialTheme.colorScheme.primary,          Icons.Default.PlayCircle, "Activa")
        "done"      -> Triple(MaterialTheme.colorScheme.secondary,        Icons.Default.CheckCircle, "Completada")
        "cancelled" -> Triple(MaterialTheme.colorScheme.error,            Icons.Default.Cancel, "Cancelada")
        else        -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Schedule, "Pendiente")
    }
    SuggestionChip(
        onClick  = {},
        modifier = modifier,
        label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon     = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors   = SuggestionChipDefaults.suggestionChipColors(
            labelColor       = color,
            iconContentColor = color,
        ),
    )
}

@Composable
private fun StopCard(stop: StopEntity, onMarkVisited: () -> Unit, onOpenVisita: () -> Unit = {}) {
    val isDone = stop.status == "done"
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape    = MaterialTheme.shapes.small,
                color    = if (isDone) MaterialTheme.colorScheme.tertiaryContainer
                           else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text  = "${stop.orderIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDone) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                stop.externalId?.let { extId ->
                    Text(
                        text  = extId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
                Text(
                    text  = stop.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                )
                stop.address?.let { addr ->
                    Text(addr, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            if (!isDone) {
                IconButton(onClick = onOpenVisita) {
                    Icon(Icons.Default.Edit,
                        contentDescription = "Registrar visita",
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onMarkVisited) {
                    Icon(Icons.Default.CheckCircleOutline,
                        contentDescription = "Marcar visitado",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(Spacing.md))
            }
        }
    }
}

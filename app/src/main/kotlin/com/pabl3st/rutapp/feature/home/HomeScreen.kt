@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.home

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onRouteClick: (String) -> Unit = {},
    onStopClick:  (String) -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val ui    by vm.ui.collectAsStateWithLifecycle()
    val today  = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("es")))
        .replaceFirstChar { it.uppercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hola, ${ui.userName}", style = MaterialTheme.typography.titleMedium)
                        Text(today, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (ui.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = vm::syncNow) {
                            Icon(Icons.Default.Sync, contentDescription = "Sincronizar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            ui.routes.isEmpty() -> EmptyRoutesMessage(Modifier.fillMaxSize().padding(padding))

            // 1 ruta hoy → mostrar JornadaBar + lista de paradas directamente
            ui.routes.size == 1 -> {
                val route = ui.routes.first()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Barra de jornada
                    item {
                        JornadaBar(routeUid = route.uid)
                    }

                    // Cabecera de la ruta
                    item {
                        RouteHeader(route = route, onRouteClick = { onRouteClick(route.uid) })
                    }

                    // Lista de paradas ordenadas por orderIndex
                    if (ui.todayStops.isEmpty()) {
                        item {
                            EmptyStopsMessage()
                        }
                    } else {
                        items(ui.todayStops, key = { it.uid }) { stop ->
                            StopRow(
                                stop    = stop,
                                onClick = { onStopClick(stop.uid) },
                            )
                        }
                    }
                }
            }

            // Varias rutas hoy → mostrar lista de rutas
            else -> RoutesList(
                routes = ui.routes,
                modifier = Modifier.padding(padding),
                onRouteClick = onRouteClick,
            )
        }
    }
}

// ── Cabecera de ruta cuando hay 1 ruta ──────────────────────
@Composable
private fun RouteHeader(route: RouteEntity, onRouteClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(route.name, style = MaterialTheme.typography.titleSmall)
            route.notes?.let {
                Text(it, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        TextButton(onClick = onRouteClick) {
            Text("Ver ruta", style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp))
        }
    }
    HorizontalDivider()
}

// ── Fila de parada en HomeScreen ─────────────────────────────
@Composable
private fun StopRow(stop: StopEntity, onClick: () -> Unit) {
    val (statusIcon, statusTint) = when (stop.status) {
        "done"     -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        "visiting" -> Icons.Default.PlayCircle   to MaterialTheme.colorScheme.tertiary
        "skipped"  -> Icons.Default.RemoveCircle  to MaterialTheme.colorScheme.outline
        else       -> Icons.Default.RadioButtonUnchecked to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (stop.status == "done")
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface,
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Número de orden
            Text(
                text  = "${stop.orderIndex + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(20.dp),
            )

            // Info
            Column(Modifier.weight(1f)) {
                Text(
                    text  = stop.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (stop.status == "done")
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                stop.address?.let { addr ->
                    Text(
                        text  = addr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (stop.status == "done" && stop.visitResult != null) {
                    Text(
                        text  = stop.visitResult,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (stop.visitResult) {
                            "contactado" -> MaterialTheme.colorScheme.primary
                            "no_estaba"  -> MaterialTheme.colorScheme.error
                            else         -> MaterialTheme.colorScheme.tertiary
                        },
                    )
                }
            }

            // Estado
            Icon(
                imageVector = statusIcon,
                contentDescription = stop.status,
                tint = statusTint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ── Lista de rutas cuando hay varias ─────────────────────────
@Composable
private fun RoutesList(
    routes: List<RouteEntity>,
    modifier: Modifier = Modifier,
    onRouteClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(routes, key = { it.uid }) { route ->
            RouteCard(route = route, onClick = { onRouteClick(route.uid) })
        }
    }
}

@Composable
private fun RouteCard(route: RouteEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Route,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(route.name, style = MaterialTheme.typography.titleSmall)
                route.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            StatusChip(status = route.status)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "active" -> "Activa"  to MaterialTheme.colorScheme.primaryContainer
        "done"   -> "Hecha"   to MaterialTheme.colorScheme.secondaryContainer
        else     -> "Pendiente" to MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.small, color = color) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun EmptyStopsMessage() {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AddLocation, null, Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Sin paradas en esta ruta",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyRoutesMessage(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Route, null, Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("Sin rutas para hoy",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Las rutas aparecerán aquí cuando estén asignadas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

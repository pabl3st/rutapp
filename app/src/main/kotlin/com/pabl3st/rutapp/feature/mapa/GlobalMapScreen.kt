@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.mapa

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.location.LocationPermissionState
import com.pabl3st.rutapp.core.location.locationPermissionState
import com.pabl3st.rutapp.core.location.rememberLocationPermissionLauncher
import com.pabl3st.rutapp.core.map.StopMapMarker
import com.pabl3st.rutapp.data.local.entity.StopTagConfig
import com.pabl3st.rutapp.data.local.entity.TagCondition
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.graphics.Color
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.local.entity.RouteEntity

// ─────────────────────────────────────────────────────────────
// GlobalMapScreen
// Pantalla de mapa del día: todos los stops de todas las rutas
// con filtros por ruta, estado y GPS
// ─────────────────────────────────────────────────────────────
@Composable
fun GlobalMapScreen(
    onNavigateToStop:  (stopUid: String)  -> Unit = {},
    onNavigateToRoute: (routeUid: String) -> Unit = {},
    vm: GlobalMapViewModel = hiltViewModel(),
) {
    val ui      by vm.ui.collectAsStateWithLifecycle()
    val context  = LocalContext.current

    // ── Permiso GPS ───────────────────────────────────────────
    val requestPermission = rememberLocationPermissionLauncher(
        onGranted = vm::onPermissionGranted,
        onDenied  = vm::onPermissionDenied,
    )
    LaunchedEffect(Unit) {
        when (context.locationPermissionState()) {
            LocationPermissionState.Granted -> vm.onPermissionGranted()
            else                            -> requestPermission()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa del día") },
                actions = {
                    if (ui.isLocating) {
                        CircularProgressIndicator(
                            modifier    = Modifier
                                .size(20.dp)
                                .padding(end = Spacing.xs),
                            strokeWidth = 2.dp,
                        )
                    } else if (ui.userLocation != null) {
                        Icon(
                            imageVector        = Icons.Default.MyLocation,
                            contentDescription = "GPS activo",
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.padding(end = Spacing.sm),
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(rememberSnackbarHostState(ui.error) { vm.clearError() })
        },
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {

            // ── Mapa (50% de la pantalla) ─────────────────────
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
            ) {
                vm.mapProvider.MapView(
                    modifier     = Modifier.fillMaxSize(),
                    config       = vm.mapConfig,
                    stops        = ui.visibleStops.filter {
                        it.latLng.lat != 0.0 && it.latLng.lng != 0.0
                    },
                    userLocation = ui.userLocation,
                    polyline     = ui.routePolyline,
                    onStopClick  = onNavigateToStop,
                    onMapClick   = {},
                    onCameraIdle = { _, _ -> },
                )
                // Badge de conteo sobre el mapa
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm),
                    shape  = MaterialTheme.shapes.small,
                    color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shadowElevation = 2.dp,
                ) {
                    Text(
                        "${ui.statDone}/${ui.statTotal}",
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Barra de resumen del día ──────────────────────
            DaySummaryRow(ui = ui)

            // ── Filtro por ruta (solo si hay >1 ruta) ─────────
            if (ui.routes.size > 1) {
                RouteFilterRow(
                    routes           = ui.routes,
                    selectedRouteUid = ui.selectedRouteUid,
                    onSelect         = vm::setRouteFilter,
                )
            }

            // ── Filtro por estado ─────────────────────────────
            StatusFilterRow(
                activeFilter = ui.activeStatusFilter,
                noGpsCount   = ui.statNoGps,
                onSelect     = vm::setStatusFilter,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Lista de stops ────────────────────────────────
            when {
                ui.isLoading -> Box(
                    modifier            = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment    = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                ui.visibleStops.isEmpty() -> EmptyStopsMessage(
                    modifier     = Modifier.fillMaxWidth().weight(1f),
                    activeFilter = ui.activeStatusFilter,
                )

                else -> LazyColumn(
                    modifier            = Modifier.fillMaxWidth().weight(1f),
                    contentPadding      = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(ui.visibleStops, key = { it.uid }) { stop ->
                        GlobalStopCard(
                            stop             = stop,
                            stopTags         = ui.stopTags,
                            onNavigateToStop = { onNavigateToStop(stop.uid) },
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo permiso GPS ───────────────────────────────────
    if (ui.showPermissionRationale) {
        AlertDialog(
            onDismissRequest = vm::dismissPermissionRationale,
            icon  = { Icon(Icons.Default.LocationOff, contentDescription = null) },
            title = { Text("GPS necesario") },
            text  = {
                Text("Para ver tu posición y ordenar los clientes por cercanía necesitas activar la ubicación.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissPermissionRationale()
                    requestPermission()
                }) { Text("Activar") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissPermissionRationale) { Text("Ahora no") }
            },
        )
    }
}

// ── Barra de estadísticas del día ─────────────────────────────
@Composable
private fun DaySummaryRow(ui: GlobalMapUiState) {
    Surface(
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(
                value = "${ui.statDone}/${ui.statTotal}",
                label = "Completadas",
                color = MaterialTheme.colorScheme.secondary,
            )
            StatItem(
                value = "${ui.statPending}",
                label = "Pendientes",
                color = MaterialTheme.colorScheme.primary,
            )
            StatItem(
                value = "${ui.statNoGps}",
                label = "Sin GPS",
                color = MaterialTheme.colorScheme.error,
            )
            if (ui.routes.size > 1) {
                StatItem(
                    value = "${ui.routes.size}",
                    label = "Rutas",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Filtro por ruta ───────────────────────────────────────────
@Composable
private fun RouteFilterRow(
    routes:           List<RouteEntity>,
    selectedRouteUid: String?,
    onSelect:         (String?) -> Unit,
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            FilterChip(
                selected = selectedRouteUid == null,
                onClick  = { onSelect(null) },
                label    = { Text("Todas", style = MaterialTheme.typography.labelSmall) },
            )
        }
        items(routes, key = { it.uid }) { route ->
            FilterChip(
                selected = selectedRouteUid == route.uid,
                onClick  = { onSelect(if (selectedRouteUid == route.uid) null else route.uid) },
                label    = {
                    Text(
                        text     = route.name,
                        style    = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

// ── Filtro por estado ─────────────────────────────────────────
@Composable
private fun StatusFilterRow(
    activeFilter: MapStatusFilter,
    noGpsCount:   Int,
    onSelect:     (MapStatusFilter) -> Unit,
) {
    data class FilterItem(val filter: MapStatusFilter, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val filters = listOf(
        FilterItem(MapStatusFilter.ALL,     "Todos",           Icons.Default.Apps),
        FilterItem(MapStatusFilter.PENDING, "Pendientes",      Icons.Default.Schedule),
        FilterItem(MapStatusFilter.DONE,    "Hechos",          Icons.Default.CheckCircle),
        FilterItem(MapStatusFilter.NO_GPS,  "Sin GPS ($noGpsCount)", Icons.Default.LocationOff),
    )
    LazyRow(
        contentPadding        = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(filters, key = { it.filter.name }) { item ->
            FilterChip(
                selected    = activeFilter == item.filter,
                onClick     = { onSelect(item.filter) },
                leadingIcon = {
                    Icon(item.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                label = {
                    Text(item.label, style = MaterialTheme.typography.labelSmall)
                },
            )
        }
    }
}

// ── Vacío cuando no hay stops para el filtro ──────────────────
@Composable
private fun EmptyStopsMessage(
    modifier:     Modifier,
    activeFilter: MapStatusFilter,
) {
    val label = when (activeFilter) {
        MapStatusFilter.PENDING -> "No hay stops pendientes"
        MapStatusFilter.DONE    -> "Ningún stop completado aún"
        MapStatusFilter.NO_GPS  -> "Todos los stops tienen GPS ✓"
        MapStatusFilter.ALL     -> "No hay stops para hoy"
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.LocationOff,
                contentDescription = null,
                modifier           = Modifier.size(40.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text  = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Card de stop en lista global ──────────────────────────────
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GlobalStopCard(
    stop:             StopMapMarker,
    stopTags:         List<StopTagConfig> = emptyList(),
    onNavigateToStop: () -> Unit,
) {
    val isDone     = stop.status == "done"
    val isVisiting = stop.status == "visiting"
    val hasGps     = stop.latLng.lat != 0.0 && stop.latLng.lng != 0.0

    // Tags evaluables desde StopMapMarker (solo condiciones de status)
    val activeTags = stopTags.filter { tag ->
        when (tag.condition) {
            TagCondition.ALWAYS          -> true
            TagCondition.STATUS_DONE     -> isDone
            TagCondition.STATUS_PENDING  -> !isDone && !isVisiting
            else                         -> false
        } && tag.enabled
    }

    val statusIcon = when (stop.status) {
        "done"     -> Icons.Default.CheckCircle
        "visiting" -> Icons.Default.Edit
        "skipped"  -> Icons.Default.Cancel
        else       -> Icons.Default.RadioButtonUnchecked
    }
    val statusColor = when (stop.status) {
        "done"     -> MaterialTheme.colorScheme.secondary
        "visiting" -> MaterialTheme.colorScheme.primary
        "skipped"  -> MaterialTheme.colorScheme.error
        else       -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = when {
            isVisiting -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            else       -> CardDefaults.cardColors()
        },
        onClick  = onNavigateToStop,
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon, stop.status, Modifier.size(20.dp), tint = statusColor)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    stop.externalId?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        stop.name, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stop.distanceLabel, style = MaterialTheme.typography.labelSmall,
                        color = if (hasGps) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                    )
                    if (!hasGps) Icon(Icons.Default.LocationOff, "Sin GPS",
                        Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
            // Tags de estado (compactos, solo si hay)
            if (activeTags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp),
                ) {
                    activeTags.forEach { tag ->
                        val bg = runCatching {
                            Color(android.graphics.Color.parseColor(tag.colorHex))
                        }.getOrDefault(Color.LightGray)
                        val fg = runCatching {
                            Color(android.graphics.Color.parseColor(tag.textColorHex))
                        }.getOrDefault(Color.Black)
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = bg) {
                            Text(tag.name, style = MaterialTheme.typography.labelSmall,
                                color = fg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Helper: Snackbar host con error reactivo ──────────────────
@Composable
private fun rememberSnackbarHostState(
    error:    String?,
    onClear:  () -> Unit,
): SnackbarHostState {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        if (error != null) {
            host.showSnackbar(error, duration = SnackbarDuration.Short)
            onClear()
        }
    }
    return host
}


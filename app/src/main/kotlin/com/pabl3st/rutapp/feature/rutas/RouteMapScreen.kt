@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.rutas

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.location.LocationPermissionState
import com.pabl3st.rutapp.core.location.locationPermissionState
import com.pabl3st.rutapp.core.location.rememberLocationPermissionLauncher
import com.pabl3st.rutapp.core.map.MapLatLng
import com.pabl3st.rutapp.core.map.MapProviderType
import com.pabl3st.rutapp.core.map.StopMapMarker

@Composable
fun RouteMapScreen(
    routeUid: String,
    onBack: () -> Unit,
    vm: RouteMapViewModel = hiltViewModel(),
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
                title = { Text(ui.routeName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Badge del proveedor activo
                    ProviderBadge(vm.mapConfig.provider)

                    // Botón optimizar orden
                    if (vm.mapProvider.supportsRouting || true) {
                        IconButton(
                            onClick  = vm::optimizeOrder,
                            enabled  = !ui.isOptimizing && ui.userLocation != null,
                        ) {
                            if (ui.isOptimizing) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, "Optimizar orden",
                                    tint = if (ui.userLocation != null)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Botón centrar en usuario
                    IconButton(
                        onClick  = { /* el mapa se centra via LaunchedEffect */ },
                        enabled  = ui.userLocation != null,
                    ) {
                        if (ui.isLocating) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, "Mi ubicación",
                                tint = if (ui.userLocation != null)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Mapa — delegado al proveedor activo ───────────
            vm.mapProvider.MapView(
                modifier     = Modifier.fillMaxWidth().weight(0.55f),
                config       = vm.mapConfig,
                stops        = ui.stops,
                userLocation = ui.userLocation,
                onStopClick  = { uid ->
                    // Scroll a la tarjeta del stop en la lista
                },
                onMapClick   = { /* nada por ahora */ },
                onCameraIdle = { _, _ -> /* guardar zoom/centro para restaurar */ },
            )

            // ── Resumen stats ─────────────────────────────────
            StopsSummaryBar(stops = ui.stops)

            // ── Lista de stops con distancia ──────────────────
            if (ui.stops.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().weight(0.45f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Place, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("Sin paradas con coordenadas GPS",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Añade coordenadas a las paradas para verlas en el mapa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxWidth().weight(0.45f),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(ui.stops, key = { it.uid }) { item ->
                        StopDistanceCard(
                            item       = item,
                            onNavigate = {
                                vm.mapProvider.openNavigation(
                                    context     = context,
                                    destination = item.latLng,
                                    label       = item.name,
                                    mode        = vm.mapConfig.route.mode,
                                )
                            },
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
            icon  = { Icon(Icons.Default.LocationOff, null) },
            title = { Text("GPS necesario") },
            text  = { Text("Para mostrar tu posición y ordenar los clientes por cercanía, RutasApp necesita acceso a tu ubicación.") },
            confirmButton = {
                TextButton(onClick = { vm.dismissPermissionRationale(); requestPermission() }) {
                    Text("Activar")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissPermissionRationale) { Text("Ahora no") }
            },
        )
    }
}

// ── Barra de resumen ──────────────────────────────────────────
@Composable
private fun StopsSummaryBar(stops: List<StopMapMarker>) {
    val total    = stops.size
    val done     = stops.count { it.status == "done" }
    val pending  = stops.count { it.status == "pending" || it.status == "visiting" }
    val skipped  = stops.count { it.status == "skipped" }
    val withGps  = stops.count { it.latLng.lat != 0.0 }

    Surface(
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryChip("$done/$total", "Completadas", MaterialTheme.colorScheme.secondary)
            SummaryChip("$pending", "Pendientes", MaterialTheme.colorScheme.primary)
            if (skipped > 0)
                SummaryChip("$skipped", "Omitidas", MaterialTheme.colorScheme.onSurfaceVariant)
            SummaryChip("$withGps", "Con GPS", MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun SummaryChip(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Badge del proveedor activo ────────────────────────────────
@Composable
private fun ProviderBadge(provider: MapProviderType) {
    val label = when (provider) {
        MapProviderType.MAPLIBRE    -> "OSM"
        MapProviderType.GOOGLE_MAPS -> "Google"
        MapProviderType.MAPBOX      -> "Mapbox"
        MapProviderType.HERE        -> "HERE"
    }
    val color = when (provider) {
        MapProviderType.MAPLIBRE    -> MaterialTheme.colorScheme.secondary
        MapProviderType.GOOGLE_MAPS -> MaterialTheme.colorScheme.primary
        MapProviderType.MAPBOX      -> MaterialTheme.colorScheme.tertiary
        MapProviderType.HERE        -> MaterialTheme.colorScheme.error
    }
    SuggestionChip(
        onClick  = {},
        label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors   = SuggestionChipDefaults.suggestionChipColors(labelColor = color),
        modifier = Modifier.padding(end = 4.dp),
    )
}

// ── Tarjeta de stop con distancia ─────────────────────────────
@Composable
private fun StopDistanceCard(
    item: StopMapMarker,
    onNavigate: () -> Unit,
) {
    val isDone    = item.status == "done"
    val isVisiting = item.status == "visiting"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = if (isVisiting) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors(),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Número de orden + distancia
            Column(
                modifier            = Modifier.width(52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (item.status) {
                        "done"     -> MaterialTheme.colorScheme.secondaryContainer
                        "visiting" -> MaterialTheme.colorScheme.primaryContainer
                        "skipped"  -> MaterialTheme.colorScheme.surfaceVariant
                        else       -> MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isDone) {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                        } else {
                            Text("${item.orderIndex + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(item.distanceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                item.externalId?.let { extId ->
                    Text(extId, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text(item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface)
            }

            // Botón navegar GPS
            if (!isDone && item.latLng.lat != 0.0) {
                IconButton(onClick = onNavigate) {
                    Icon(Icons.Default.Navigation, "Navegar",
                        modifier = Modifier.size(20.dp),
                        tint     = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

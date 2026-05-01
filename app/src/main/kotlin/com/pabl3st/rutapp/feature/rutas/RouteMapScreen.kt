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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.pabl3st.rutapp.core.location.LocationPermissionState
import com.pabl3st.rutapp.core.location.locationPermissionState
import com.pabl3st.rutapp.core.location.rememberLocationPermissionLauncher

@Composable
fun RouteMapScreen(
    routeUid: String,
    onBack: () -> Unit,
    vm: RouteMapViewModel = hiltViewModel(),
) {
    val ui      by vm.ui.collectAsStateWithLifecycle()
    val context  = LocalContext.current

    // ── Solicitar permiso GPS al entrar ───────────────────────
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

    // ── Cámara del mapa ───────────────────────────────────────
    val cameraPositionState = rememberCameraPositionState()

    // Centrar en usuario o en primer stop con coords
    val firstStopLatLng = ui.stops.firstOrNull { it.stop.lat != null }
        ?.stop?.let { LatLng(it.lat!!, it.lng!!) }

    LaunchedEffect(firstStopLatLng, ui.userLocation) {
        val target = ui.userLocation ?: firstStopLatLng ?: return@LaunchedEffect
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 13f))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.routeName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (ui.isLocating) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                    } else {
                        IconButton(onClick = {
                            val target = ui.userLocation ?: return@IconButton
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(target, 15f))
                        }) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Google Map ────────────────────────────────────
            GoogleMap(
                modifier            = Modifier.fillMaxWidth().weight(0.55f),
                cameraPositionState = cameraPositionState,
                uiSettings          = MapUiSettings(
                    zoomControlsEnabled     = false,
                    myLocationButtonEnabled = false,
                    compassEnabled          = true,
                ),
                properties = MapProperties(
                    isMyLocationEnabled = ui.locationPermissionGranted,
                    mapType             = MapType.NORMAL,
                ),
            ) {
                ui.stops.forEach { item ->
                    val stop = item.stop
                    if (stop.lat != null && stop.lng != null) {
                        val isDone = stop.status == "done"
                        Marker(
                            state   = MarkerState(LatLng(stop.lat, stop.lng)),
                            title   = stop.name,
                            snippet = "${item.distanceLabel} · ${stop.status}",
                            icon    = BitmapDescriptorFactory.defaultMarker(
                                if (isDone) BitmapDescriptorFactory.HUE_GREEN
                                else        BitmapDescriptorFactory.HUE_AZURE
                            ),
                        )
                    }
                }
            }

            // ── Lista stops con distancia ─────────────────────
            if (ui.stops.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().weight(0.45f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Place, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("Sin paradas con ubicación GPS",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxWidth().weight(0.45f),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(ui.stops, key = { it.stop.uid }) { item ->
                        StopDistanceCard(
                            item      = item,
                            onCenter  = {
                                item.stop.lat?.let { lat ->
                                    item.stop.lng?.let { lng ->
                                        cameraPositionState.move(
                                            CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16f)
                                        )
                                    }
                                }
                            },
                            onNavigate = {
                                val lat = item.stop.lat ?: return@StopDistanceCard
                                val lng = item.stop.lng ?: return@StopDistanceCard
                                val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    // Fallback navegador web
                                    context.startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://maps.google.com/?daddr=$lat,$lng")))
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo permiso denegado ──────────────────────────────
    if (ui.showPermissionRationale) {
        AlertDialog(
            onDismissRequest = vm::dismissPermissionRationale,
            icon  = { Icon(Icons.Default.LocationOff, null) },
            title = { Text("GPS necesario") },
            text  = { Text("Para mostrar tu posición y calcular distancias a los clientes, RutasApp necesita acceso a tu ubicación.") },
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

@Composable
private fun StopDistanceCard(
    item: StopWithDistance,
    onCenter: () -> Unit,
    onNavigate: () -> Unit,
) {
    val isDone = item.stop.status == "done"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Indicador distancia + icono estado
            Column(
                modifier            = Modifier.width(52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector        = if (isDone) Icons.Default.CheckCircle else Icons.Default.Place,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp),
                    tint               = if (isDone) MaterialTheme.colorScheme.secondary
                                         else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = item.distanceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                item.stop.externalId?.let { extId ->
                    Text(extId, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text  = item.stop.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                )
                item.stop.address?.let { addr ->
                    Text(addr, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }

            // Centrar en mapa
            if (item.stop.lat != null) {
                IconButton(onClick = onCenter) {
                    Icon(Icons.Default.CenterFocusWeak, "Centrar",
                        modifier = Modifier.size(18.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Navegar con GPS
            if (item.stop.lat != null && !isDone) {
                IconButton(onClick = onNavigate) {
                    Icon(Icons.Default.Navigation, "Navegar",
                        modifier = Modifier.size(18.dp),
                        tint     = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

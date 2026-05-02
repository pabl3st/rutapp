package com.pabl3st.rutapp.core.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Google Maps Provider
 * Requiere API key en Google Cloud Console con Maps SDK for Android habilitado.
 * Activar desde Perfil → Ajustes → Proveedor de mapas → Google Maps + pegar key.
 *
 * Ventajas vs MapLibre:
 * - Datos de tráfico en tiempo real
 * - Street View
 * - Places Autocomplete
 * - Mejor cobertura global en zonas rurales
 *
 * Desventajas:
 * - Requiere facturación Google Cloud
 * - ~7€/1000 cargas mapa pasado el free tier
 * - Sin offline
 */
class GoogleMapsProvider : MapProvider {

    override val type              = MapProviderType.GOOGLE_MAPS
    override val supportsOffline   = false
    override val supportsTraffic   = true
    override val supportsRouting   = true
    override val supportsSatellite = true

    @Composable
    override fun MapView(
        modifier: Modifier,
        config: MapConfig,
        stops: List<StopMapMarker>,
        userLocation: MapLatLng?,
        polyline: List<MapLatLng>,
        onStopClick: (uid: String) -> Unit,
        onMapClick: (MapLatLng) -> Unit,
        onCameraIdle: (center: MapLatLng, zoom: Float) -> Unit,
    ) {
        // TODO: implementar cuando el usuario configure su API key
        // Requiere: implementation("com.google.maps.android:maps-compose:4.3.3")
        //           implementation("com.google.android.gms:play-services-maps:19.0.0")
        NotConfiguredPlaceholder(
            providerName = "Google Maps",
            requiresKey  = true,
            keyHint      = "Obtener en console.cloud.google.com → APIs → Maps SDK for Android",
        )
    }

    override fun openNavigation(context: Context, destination: MapLatLng, label: String?, mode: RouteMode) {
        val modeParam = when (mode) {
            RouteMode.DRIVING -> "d"; RouteMode.WALKING -> "w"
            RouteMode.CYCLING -> "b"; RouteMode.TRANSIT -> "r"
        }
        val uri = Uri.parse("google.navigation:q=${destination.lat},${destination.lng}&mode=$modeParam")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        })
    }

    override suspend fun calculateRoute(origin: MapLatLng, stops: List<MapLatLng>, options: RouteOptions): MapRoute? = null
    override suspend fun optimizeStopOrder(origin: MapLatLng, stops: List<StopMapMarker>, options: RouteOptions) = stops
    override suspend fun geocode(address: String): MapLatLng? = null
    override suspend fun reverseGeocode(location: MapLatLng): String? = null
    override suspend fun downloadOfflineRegion(name: String, bounds: Pair<MapLatLng, MapLatLng>, minZoom: Float, maxZoom: Float) = false
    override suspend fun deleteOfflineRegion(name: String) = false
    override suspend fun getOfflineRegions(): List<String> = emptyList()
}

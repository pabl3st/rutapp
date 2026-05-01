package com.pabl3st.rutapp.core.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Mapbox Provider
 * Requiere token público en mapbox.com
 *
 * Ventajas vs MapLibre:
 * - Mapas offline descargables (killer feature para comerciales sin cobertura)
 * - Routing matrix (calcular todas las combinaciones de distancias entre stops)
 * - Isochrones API (qué puntos puedo alcanzar en X minutos desde aquí)
 * - Turn-by-turn navigation integrada en el SDK
 * - Mejor calidad visual de los mapas
 * - Soporte para custom map styles con Mapbox Studio
 *
 * Precio: 50.000 cargas/mes gratis, luego ~5€/1000
 * Token: en account.mapbox.com → Tokens → Create a token
 */
class MapboxProvider : MapProvider {

    override val type              = MapProviderType.MAPBOX
    override val supportsOffline   = true
    override val supportsTraffic   = true
    override val supportsRouting   = true
    override val supportsSatellite = true

    @Composable
    override fun MapView(
        modifier: Modifier,
        config: MapConfig,
        stops: List<StopMapMarker>,
        userLocation: MapLatLng?,
        onStopClick: (uid: String) -> Unit,
        onMapClick: (MapLatLng) -> Unit,
        onCameraIdle: (center: MapLatLng, zoom: Float) -> Unit,
    ) {
        // TODO: implementar con Maps SDK for Android de Mapbox
        // Requiere: implementation("com.mapbox.maps:android:11.x.x")
        NotConfiguredPlaceholder(
            providerName = "Mapbox",
            requiresKey  = true,
            keyHint      = "Obtener en account.mapbox.com → Access tokens",
        )
    }

    override fun openNavigation(context: Context, destination: MapLatLng, label: String?, mode: RouteMode) {
        // Intenta abrir Mapbox Navigation, fallback a geo URI
        val uri = Uri.parse("mapbox://directions?access_token=&destination=${destination.lng},${destination.lat}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:${destination.lat},${destination.lng}")))
        }
    }

    override suspend fun calculateRoute(origin: MapLatLng, stops: List<MapLatLng>, options: RouteOptions): MapRoute? = null
    override suspend fun optimizeStopOrder(origin: MapLatLng, stops: List<StopMapMarker>, options: RouteOptions) = stops
    override suspend fun geocode(address: String): MapLatLng? = null
    override suspend fun reverseGeocode(location: MapLatLng): String? = null
    override suspend fun downloadOfflineRegion(name: String, bounds: Pair<MapLatLng, MapLatLng>, minZoom: Float, maxZoom: Float) = false
    override suspend fun deleteOfflineRegion(name: String) = false
    override suspend fun getOfflineRegions(): List<String> = emptyList()
}

/**
 * HERE Maps Provider
 * Requiere API key en developer.here.com
 *
 * Ventajas vs otros:
 * - Routing optimizado para flotas y logística
 * - Truck routing (restricciones de peso, altura, peligrosos)
 * - Isoline routing (áreas alcanzables en tiempo/distancia)
 * - Indoor maps (aeropuertos, centros comerciales)
 * - Real-time traffic incidents
 * - HERE Weather integrado
 * - El más generoso: 250.000 transacciones/mes gratis
 *
 * Ideal para: distribución alimentación, logística, rutas largas
 * Token: en developer.here.com → Projects → OAuth → Create API key
 */
class HereMapsProvider : MapProvider {

    override val type              = MapProviderType.HERE
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
        onStopClick: (uid: String) -> Unit,
        onMapClick: (MapLatLng) -> Unit,
        onCameraIdle: (center: MapLatLng, zoom: Float) -> Unit,
    ) {
        // TODO: implementar con HERE Maps SDK for Android
        // Requiere: implementation("com.here.sdk:mapview-lite:4.x.x")
        NotConfiguredPlaceholder(
            providerName = "HERE Maps",
            requiresKey  = true,
            keyHint      = "Obtener en developer.here.com → Projects → API Keys",
        )
    }

    override fun openNavigation(context: Context, destination: MapLatLng, label: String?, mode: RouteMode) {
        // HERE WeGo app
        val hereUri = Uri.parse("here-route://${destination.lat},${destination.lng}/${label ?: "Destino"}")
        val intent  = Intent(Intent.ACTION_VIEW, hereUri)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:${destination.lat},${destination.lng}")))
        }
    }

    override suspend fun calculateRoute(origin: MapLatLng, stops: List<MapLatLng>, options: RouteOptions): MapRoute? = null
    override suspend fun optimizeStopOrder(origin: MapLatLng, stops: List<StopMapMarker>, options: RouteOptions) = stops
    override suspend fun geocode(address: String): MapLatLng? = null
    override suspend fun reverseGeocode(location: MapLatLng): String? = null
    override suspend fun downloadOfflineRegion(name: String, bounds: Pair<MapLatLng, MapLatLng>, minZoom: Float, maxZoom: Float) = false
    override suspend fun deleteOfflineRegion(name: String) = false
    override suspend fun getOfflineRegions(): List<String> = emptyList()
}

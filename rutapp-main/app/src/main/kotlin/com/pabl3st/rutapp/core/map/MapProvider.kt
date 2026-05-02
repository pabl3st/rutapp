package com.pabl3st.rutapp.core.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// ── Tipos de datos compartidos entre proveedores ──────────────

data class MapLatLng(val lat: Double, val lng: Double)

data class StopMapMarker(
    val uid: String,
    val name: String,
    val externalId: String?,
    val latLng: MapLatLng,
    val status: String,           // pending|visiting|done|skipped
    val distanceLabel: String,
    val orderIndex: Int,
)

data class MapRoute(
    val points: List<MapLatLng>,  // polilínea del recorrido óptimo
    val distanceMeters: Double,
    val durationSeconds: Int,
)

// ── Opciones de estilo del mapa ───────────────────────────────

enum class MapStyle {
    STANDARD,      // mapa estándar calles
    SATELLITE,     // vista satélite
    HYBRID,        // satélite + calles encima
    TERRAIN,       // relieve y topografía
    DARK,          // modo oscuro (compatible con tema app)
    LIGHT,         // modo claro
    TRAFFIC,       // con capa de tráfico en tiempo real
    NAVIGATION,    // optimizado para conducir (sans serif, alto contraste)
}

// ── Opciones de capas adicionales ────────────────────────────

data class MapLayerOptions(
    val showTraffic: Boolean         = false,   // capa de tráfico
    val showTransit: Boolean         = false,   // transporte público
    val showBuildings3d: Boolean     = false,   // edificios 3D
    val showBicycleRoutes: Boolean   = false,   // carriles bici
    val showPOIs: Boolean            = true,    // puntos de interés
    val showCompass: Boolean         = true,    // brújula
    val showScaleBar: Boolean        = true,    // barra de escala
    val showZoomControls: Boolean    = false,   // botones zoom
    val showMyLocationButton: Boolean = false,  // botón "ir a mi posición"
)

// ── Opciones de clustering de markers ────────────────────────

data class ClusterOptions(
    val enabled: Boolean      = true,
    val minZoom: Float        = 10f,   // zoom mínimo para activar clustering
    val radius: Int           = 50,    // radio en dp para agrupar markers
    val maxZoom: Float        = 14f,   // zoom máximo — a partir de aquí markers individuales
)

// ── Opciones de routing ───────────────────────────────────────

enum class RouteMode {
    DRIVING,       // coche
    WALKING,       // a pie
    CYCLING,       // bici
    TRANSIT,       // transporte público
}

data class RouteOptions(
    val mode: RouteMode            = RouteMode.DRIVING,
    val avoidTolls: Boolean        = false,
    val avoidHighways: Boolean     = false,
    val optimizeOrder: Boolean     = false,   // TSP — reordenar stops para mínima distancia
    val showPolyline: Boolean      = true,    // trazar ruta en el mapa
    val polylineColor: Long        = 0xFF2563EB,
    val polylineWidth: Float       = 4f,
)

// ── Opciones de cámara ────────────────────────────────────────

data class CameraOptions(
    val initialZoom: Float         = 13f,
    val minZoom: Float             = 3f,
    val maxZoom: Float             = 20f,
    val tiltEnabled: Boolean       = true,    // inclinación 3D
    val rotationEnabled: Boolean   = true,    // rotación con dos dedos
    val scrollEnabled: Boolean     = true,
    val zoomEnabled: Boolean       = true,
    val padding: MapPadding        = MapPadding(),
)

data class MapPadding(
    val top: Int    = 0,
    val bottom: Int = 0,
    val left: Int   = 0,
    val right: Int  = 0,
)

// ── Opciones de marcadores ────────────────────────────────────

enum class MarkerStyle {
    PIN,           // pin clásico
    DOT,           // punto pequeño
    NUMBERED,      // con número de orden encima
    CUSTOM_ICON,   // icono SVG/bitmap custom
}

data class MarkerOptions(
    val style: MarkerStyle         = MarkerStyle.NUMBERED,
    val pendingColor: Long         = 0xFF2563EB,   // azul primary
    val visitingColor: Long        = 0xFFF59E0B,   // ámbar
    val doneColor: Long            = 0xFF10B981,   // verde
    val skippedColor: Long         = 0xFF94A3B8,   // gris
    val showLabel: Boolean         = true,         // nombre del stop en el pin
    val showExternalId: Boolean    = true,         // código cliente en el pin
)

// ── Configuración completa del proveedor ─────────────────────

data class MapConfig(
    val provider: MapProviderType          = MapProviderType.MAPLIBRE,
    val style: MapStyle                    = MapStyle.STANDARD,
    val layers: MapLayerOptions            = MapLayerOptions(),
    val camera: CameraOptions              = CameraOptions(),
    val markers: MarkerOptions             = MarkerOptions(),
    val cluster: ClusterOptions            = ClusterOptions(),
    val route: RouteOptions                = RouteOptions(),
    val apiKey: String?                    = null,   // Google Maps / Mapbox / HERE
    val offlineCacheEnabled: Boolean       = false,  // Mapbox offline
    val offlineCacheRegionName: String?    = null,
    val darkModeFollowSystem: Boolean      = true,   // adaptar estilo al tema del sistema
)

enum class MapProviderType {
    MAPLIBRE,      // OSM, gratuito, offline-capable — DEFAULT
    GOOGLE_MAPS,   // requiere API key Google Cloud
    MAPBOX,        // requiere token Mapbox, mejor offline
    HERE,          // requiere API key HERE, mejor logística
}

// ── Interfaz principal ────────────────────────────────────────

interface MapProvider {

    val type: MapProviderType
    val supportsOffline: Boolean
    val supportsTraffic: Boolean
    val supportsRouting: Boolean
    val supportsSatellite: Boolean

    // ── Vista del mapa ────────────────────────────────────────
    @Composable
    fun MapView(
        modifier: Modifier,
        config: MapConfig,
        stops: List<StopMapMarker>,
        userLocation: MapLatLng?,
        polyline: List<MapLatLng> = emptyList(),
        onStopClick: (uid: String) -> Unit,
        onMapClick: (MapLatLng) -> Unit,
        onCameraIdle: (center: MapLatLng, zoom: Float) -> Unit,
    )

    // ── Navegación externa al stop ────────────────────────────
    fun openNavigation(
        context: Context,
        destination: MapLatLng,
        label: String?,
        mode: RouteMode = RouteMode.DRIVING,
    )

    // ── Calcular ruta entre stops ─────────────────────────────
    // Devuelve null si el proveedor no soporta routing
    suspend fun calculateRoute(
        origin: MapLatLng,
        stops: List<MapLatLng>,
        options: RouteOptions,
    ): MapRoute?

    // ── Optimizar orden de stops (TSP) ────────────────────────
    suspend fun optimizeStopOrder(
        origin: MapLatLng,
        stops: List<StopMapMarker>,
        options: RouteOptions,
    ): List<StopMapMarker>

    // ── Geocoding — dirección → coords ────────────────────────
    suspend fun geocode(address: String): MapLatLng?

    // ── Geocoding inverso — coords → dirección ────────────────
    suspend fun reverseGeocode(location: MapLatLng): String?

    // ── Cache offline ─────────────────────────────────────────
    suspend fun downloadOfflineRegion(
        name: String,
        bounds: Pair<MapLatLng, MapLatLng>,
        minZoom: Float,
        maxZoom: Float,
    ): Boolean

    suspend fun deleteOfflineRegion(name: String): Boolean
    suspend fun getOfflineRegions(): List<String>
}

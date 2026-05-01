package com.pabl3st.rutapp.core.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.MarkerOptions

/**
 * Proveedor MapLibre — OpenStreetMap
 * - Sin API key ni cuenta necesaria
 * - Gratuito, sin límites de uso
 * - Offline-capable (descargar tiles)
 * - Código abierto (fork de Mapbox GL)
 */
class MapLibreProvider(private val context: Context) : MapProvider {

    override val type             = MapProviderType.MAPLIBRE
    override val supportsOffline  = true
    override val supportsTraffic  = false   // OSM no tiene datos de tráfico en tiempo real
    override val supportsRouting  = false   // routing requiere servidor OSRM (S07+)
    override val supportsSatellite = true   // via Esri / Stadia tiles

    // ── Estilos de tile disponibles (sin key) ────────────────
    private fun styleUrl(style: MapStyle, darkMode: Boolean): String = when (style) {
        MapStyle.DARK       -> "https://tiles.openfreemap.org/styles/dark"
        MapStyle.LIGHT      -> "https://tiles.openfreemap.org/styles/bright"
        MapStyle.STANDARD   -> if (darkMode) "https://tiles.openfreemap.org/styles/dark"
                               else "https://tiles.openfreemap.org/styles/bright"
        MapStyle.SATELLITE  -> "https://demotiles.maplibre.org/satellite-style.json"
        MapStyle.HYBRID     -> "https://demotiles.maplibre.org/satellite-style.json"
        MapStyle.TERRAIN    -> "https://demotiles.maplibre.org/style.json"
        MapStyle.TRAFFIC    -> "https://tiles.openfreemap.org/styles/bright"  // sin tráfico
        MapStyle.NAVIGATION -> "https://tiles.openfreemap.org/styles/bright"
    }

    // ── Colores de marker por estado ──────────────────────────
    private fun markerColor(status: String, opts: MarkerOptions): String = when (status) {
        "done"     -> "#${opts.doneColor.toString(16).takeLast(6)}"
        "visiting" -> "#${opts.visitingColor.toString(16).takeLast(6)}"
        "skipped"  -> "#${opts.skippedColor.toString(16).takeLast(6)}"
        else       -> "#${opts.pendingColor.toString(16).takeLast(6)}"
    }

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
        val context = LocalContext.current
        val isDark  = !androidx.compose.foundation.isSystemInDarkTheme()

        // Inicializar MapLibre una sola vez
        LaunchedEffect(Unit) {
            MapLibre.getInstance(context)
        }

        var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

        AndroidView(
            modifier = modifier,
            factory  = { ctx ->
                MapView(ctx).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        mapLibreMap = map

                        // Estilo
                        val url = styleUrl(config.style, isDark)
                        map.setStyle(url)

                        // Opciones de cámara
                        map.uiSettings.apply {
                            isCompassEnabled        = config.layers.showCompass
                            isZoomControlsEnabled   = config.layers.showZoomControls
                            isRotateGesturesEnabled = config.camera.rotationEnabled
                            isTiltGesturesEnabled   = config.camera.tiltEnabled
                            isScrollGesturesEnabled = config.camera.scrollEnabled
                            isZoomGesturesEnabled   = config.camera.zoomEnabled
                        }

                        map.setMinZoomPreference(config.camera.minZoom.toDouble())
                        map.setMaxZoomPreference(config.camera.maxZoom.toDouble())

                        // Listener click en mapa
                        map.addOnMapClickListener { latLng ->
                            onMapClick(MapLatLng(latLng.latitude, latLng.longitude))
                            true
                        }

                        // Listener camera idle
                        map.addOnCameraIdleListener {
                            val pos = map.cameraPosition
                            onCameraIdle(
                                MapLatLng(pos.target.latitude, pos.target.longitude),
                                pos.zoom.toFloat()
                            )
                        }

                        // Listener click en marker
                        map.setOnMarkerClickListener { marker ->
                            val uid = marker.snippet ?: return@setOnMarkerClickListener false
                            onStopClick(uid)
                            true
                        }

                        // Centrar cámara
                        val target = userLocation?.let { LatLng(it.lat, it.lng) }
                            ?: stops.firstOrNull { it.status != "done" }
                                ?.let { LatLng(it.latLng.lat, it.latLng.lng) }

                        target?.let {
                            map.moveCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(it)
                                        .zoom(config.camera.initialZoom.toDouble())
                                        .build()
                                )
                            )
                        }

                        // Añadir markers
                        addStopMarkers(map, stops, config.markers)
                    }
                }
            },
            update = { mapView ->
                // Actualizar markers cuando cambian los stops
                mapLibreMap?.let { map ->
                    map.clear()
                    addStopMarkers(map, stops, config.markers)

                    // Actualizar posición del usuario si cambió
                    userLocation?.let { loc ->
                        // El punto azul "mi ubicación" lo gestiona el SDK
                        // con isMyLocationEnabled si hay permiso
                    }
                }
            }
        )
    }

    private fun addStopMarkers(
        map: MapLibreMap,
        stops: List<StopMapMarker>,
        opts: MarkerOptions,
    ) {
        stops.forEach { stop ->
            val label = when {
                opts.showExternalId && stop.externalId != null ->
                    "${stop.externalId} · ${stop.name}"
                opts.style == MarkerStyle.NUMBERED ->
                    "${stop.orderIndex + 1}. ${stop.name}"
                else -> stop.name
            }

            map.addMarker(
                MarkerOptions()
                    .position(LatLng(stop.latLng.lat, stop.latLng.lng))
                    .title(label)
                    .snippet(stop.uid)   // uid en snippet para recuperarlo en el click listener
            )
        }
    }

    // ── Navegación — abre la app de navegación del sistema ────
    override fun openNavigation(
        context: Context,
        destination: MapLatLng,
        label: String?,
        mode: RouteMode,
    ) {
        val modeParam = when (mode) {
            RouteMode.DRIVING  -> "d"
            RouteMode.WALKING  -> "w"
            RouteMode.CYCLING  -> "b"
            RouteMode.TRANSIT  -> "r"
        }

        // Intentar Google Maps primero, fallback a cualquier app de navegación
        val gMapsUri = Uri.parse(
            "google.navigation:q=${destination.lat},${destination.lng}&mode=$modeParam"
        )
        val gMapsIntent = Intent(Intent.ACTION_VIEW, gMapsUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (gMapsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(gMapsIntent)
        } else {
            // Fallback: geo URI universal (lo abre cualquier app de mapas instalada)
            val geoUri = Uri.parse(
                "geo:${destination.lat},${destination.lng}?q=${destination.lat},${destination.lng}" +
                (label?.let { "(${Uri.encode(it)})" } ?: "")
            )
            val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
            if (geoIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(geoIntent)
            } else {
                // Último fallback: navegador web con Google Maps
                context.startActivity(
                    Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?daddr=${destination.lat},${destination.lng}"))
                )
            }
        }
    }

    // ── Routing — OSM no tiene servidor incluido ──────────────
    // Se implementará en S07 con OSRM público o servidor propio
    override suspend fun calculateRoute(
        origin: MapLatLng,
        stops: List<MapLatLng>,
        options: RouteOptions,
    ): MapRoute? = null   // TODO S07

    override suspend fun optimizeStopOrder(
        origin: MapLatLng,
        stops: List<StopMapMarker>,
        options: RouteOptions,
    ): List<StopMapMarker> {
        // Algoritmo greedy simple: nearest neighbor
        // Suficiente para rutas de hasta 20 stops
        if (stops.size <= 1) return stops

        val remaining = stops.toMutableList()
        val ordered   = mutableListOf<StopMapMarker>()
        var current   = origin

        while (remaining.isNotEmpty()) {
            val nearest = remaining.minByOrNull { stop ->
                val dx = stop.latLng.lat - current.lat
                val dy = stop.latLng.lng - current.lng
                dx * dx + dy * dy  // distancia euclidiana — suficiente para ordenar
            } ?: break
            ordered.add(nearest)
            remaining.remove(nearest)
            current = nearest.latLng
        }
        return ordered
    }

    // ── Geocoding — Nominatim (OSM, gratuito) ────────────────
    override suspend fun geocode(address: String): MapLatLng? = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = Uri.encode(address)
            val url = java.net.URL(
                "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1"
            )
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "RutasApp Android")
            val json = conn.inputStream.bufferedReader().readText()
            val arr  = org.json.JSONArray(json)
            if (arr.length() == 0) return@runCatching null
            val obj  = arr.getJSONObject(0)
            MapLatLng(obj.getDouble("lat"), obj.getDouble("lon"))
        }.getOrNull()
    }

    // ── Geocoding inverso — Nominatim ─────────────────────────
    override suspend fun reverseGeocode(location: MapLatLng): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = java.net.URL(
                "https://nominatim.openstreetmap.org/reverse?lat=${location.lat}&lon=${location.lng}&format=json"
            )
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "RutasApp Android")
            val json = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
            json.optString("display_name").ifEmpty { null }
        }.getOrNull()
    }

    // ── Offline — MapLibre soporta cache de tiles ─────────────
    override suspend fun downloadOfflineRegion(
        name: String,
        bounds: Pair<MapLatLng, MapLatLng>,
        minZoom: Float,
        maxZoom: Float,
    ): Boolean {
        // TODO S07 — implementar con OfflineManager de MapLibre
        return false
    }

    override suspend fun deleteOfflineRegion(name: String): Boolean = false
    override suspend fun getOfflineRegions(): List<String> = emptyList()
}

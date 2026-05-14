package com.pabl3st.rutapp.core.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MLLatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
// Alias explícito para evitar conflicto con nuestra MarkerOptions
import org.maplibre.android.annotations.CircleOptions as MLCircleOptions
import org.maplibre.android.annotations.MarkerOptions as MLMarkerOptions
import org.maplibre.android.annotations.PolylineOptions as MLPolylineOptions

/**
 * MapLibre Provider — OpenStreetMap
 * Sin API key, sin cuenta, sin límites, offline-capable.
 */
@Suppress("DEPRECATION")
class MapLibreProvider(private val context: Context) : MapProvider {

    override val type             = MapProviderType.MAPLIBRE
    override val supportsOffline  = true
    override val supportsTraffic  = false
    override val supportsRouting  = false
    override val supportsSatellite = true

    private fun styleUrl(style: MapStyle, darkMode: Boolean): String = when (style) {
        MapStyle.DARK                  -> "https://tiles.openfreemap.org/styles/dark"
        MapStyle.SATELLITE,
        MapStyle.HYBRID                -> "https://tiles.openfreemap.org/styles/bright"
        MapStyle.LIGHT,
        MapStyle.STANDARD,
        MapStyle.TERRAIN,
        MapStyle.TRAFFIC,
        MapStyle.NAVIGATION            ->
            if (darkMode) "https://tiles.openfreemap.org/styles/dark"
            else          "https://tiles.openfreemap.org/styles/liberty"
    }

    // Convierte Long de color (0xFF2563EB) a string hex "#2563eb"
    private fun Long.toHexColor(): String = "#%06x".format(this and 0xFFFFFF)

    private fun markerColorHex(status: String, opts: com.pabl3st.rutapp.core.map.MarkerOptions): String = when (status) {
        "done"     -> opts.doneColor.toHexColor()
        "visiting" -> opts.visitingColor.toHexColor()
        "skipped"  -> opts.skippedColor.toHexColor()
        else       -> opts.pendingColor.toHexColor()
    }

    // Cache de la última localización para detectar cuándo cambia
    private var _lastCenteredLocation: MapLatLng? = null
    private var _mapReadyRef: MapLibreMap? = null

    fun centerOnLocation(loc: MapLatLng) {
        _mapReadyRef?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(MLLatLng(loc.lat, loc.lng))
                    .zoom(15.0)
                    .build()
            ), 600
        )
    }

    fun fitBoundsToStops(stops: List<StopMapMarker>) {
        val pts = stops.filter { it.latLng.lat != 0.0 && it.latLng.lng != 0.0 }
        if (pts.isEmpty()) return
        val map = _mapReadyRef ?: return
        if (pts.size == 1) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                MLLatLng(pts[0].latLng.lat, pts[0].latLng.lng), 14.0), 600)
            return
        }
        val latMin = pts.minOf { it.latLng.lat }
        val latMax = pts.maxOf { it.latLng.lat }
        val lngMin = pts.minOf { it.latLng.lng }
        val lngMax = pts.maxOf { it.latLng.lng }
        val sw = MLLatLng(latMin, lngMin)
        val ne = MLLatLng(latMax, lngMax)
        val bounds = org.maplibre.android.geometry.LatLngBounds.from(
            ne.latitude, ne.longitude, sw.latitude, sw.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 800)
    }

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
        val ctx    = LocalContext.current
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()

        remember(ctx) { MapLibre.getInstance(ctx) }

        var mlMap by remember { mutableStateOf<MapLibreMap?>(null) }
        // Rastrear si ya hemos hecho el fit inicial (una sola vez por composición)
        var didInitialFit by remember { mutableStateOf(false) }

        // Fit bounds a los stops cuando lleguen (corrección: el update{} puede
        // ejecutarse antes de que el style esté cargado)
        LaunchedEffect(stops) {
            val map = _mapReadyRef ?: return@LaunchedEffect
            if (!didInitialFit && stops.any { it.latLng.lat != 0.0 && it.latLng.lng != 0.0 }) {
                // Pequeño delay para asegurar que el style está completamente listo
                kotlinx.coroutines.delay(300)
                val mapNow = _mapReadyRef ?: return@LaunchedEffect
                if (mapNow.style?.isFullyLoaded == true) {
                    val pts = stops.filter { it.latLng.lat != 0.0 && it.latLng.lng != 0.0 }
                    if (pts.size >= 2) {
                        val latMin = pts.minOf { it.latLng.lat }
                        val latMax = pts.maxOf { it.latLng.lat }
                        val lngMin = pts.minOf { it.latLng.lng }
                        val lngMax = pts.maxOf { it.latLng.lng }
                        mapNow.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                org.maplibre.android.geometry.LatLngBounds.from(latMax, lngMax, latMin, lngMin), 80
                            ), 800
                        )
                        didInitialFit = true
                    } else if (pts.size == 1) {
                        mapNow.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(MLLatLng(pts[0].latLng.lat, pts[0].latLng.lng), 14.0), 600
                        )
                        didInitialFit = true
                    }
                }
            }
        }

        // Cuando llega una nueva localización del usuario → centrar automáticamente
        // Solo la primera vez que llega, o cuando cambia radicalmente (> 50 km)
        LaunchedEffect(userLocation) {
            val map = mlMap ?: return@LaunchedEffect
            val loc = userLocation ?: return@LaunchedEffect
            val prev = _lastCenteredLocation
            val distFar = prev == null || run {
                val dlat = loc.lat - prev.lat; val dlng = loc.lng - prev.lng
                (dlat * dlat + dlng * dlng) > 0.2  // ~50 km
            }
            if (distFar) {
                _lastCenteredLocation = loc
                map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(MLLatLng(loc.lat, loc.lng))
                            .zoom(14.0)
                            .build()
                    ), 600
                )
            }
        }

        AndroidView(
            modifier = modifier,
            factory  = { factoryCtx ->
                MapView(factoryCtx).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        mlMap = map
                        _mapReadyRef = map

                        map.setStyle(styleUrl(config.style, isDark)) {
                            // Cámara inicial: prioridad → stops con GPS → Madrid fallback
                            // La localización GPS llega después via LaunchedEffect
                            val firstStop = stops.firstOrNull {
                                it.status != "done" && it.latLng.lat != 0.0 && it.latLng.lng != 0.0
                            }
                            val target = firstStop?.let { MLLatLng(it.latLng.lat, it.latLng.lng) }
                                ?: MLLatLng(40.4168, -3.7038)

                            map.moveCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(target)
                                        .zoom(config.camera.initialZoom.toDouble())
                                        .build()
                                )
                            )
                            addStopMarkers(map, stops, config.markers)
                        }

                        map.uiSettings.apply {
                            isCompassEnabled        = config.layers.showCompass
                            isRotateGesturesEnabled = config.camera.rotationEnabled
                            isTiltGesturesEnabled   = config.camera.tiltEnabled
                            isScrollGesturesEnabled = config.camera.scrollEnabled
                            isZoomGesturesEnabled   = config.camera.zoomEnabled
                        }
                        map.setMinZoomPreference(config.camera.minZoom.toDouble())
                        map.setMaxZoomPreference(config.camera.maxZoom.toDouble())

                        map.addOnMapClickListener { latLng ->
                            onMapClick(MapLatLng(latLng.latitude, latLng.longitude)); true
                        }
                        map.addOnCameraIdleListener {
                            map.cameraPosition.target?.let { t ->
                                onCameraIdle(MapLatLng(t.latitude, t.longitude), map.cameraPosition.zoom.toFloat())
                            }
                        }
                        map.setOnMarkerClickListener { marker ->
                            marker.snippet?.let { onStopClick(it) }
                            true
                        }
                    }
                }
            },
            update = { _ ->
                mlMap?.let { map ->
                    map.clear()
                    addStopMarkers(map, stops, config.markers)
                    // Posición del usuario: círculo azul (distinto visualmente del pin rojo de stops)
                    userLocation?.let { loc ->
                        if (loc.lat != 0.0 && loc.lng != 0.0) {
                            map.addCircle(
                                MLCircleOptions()
                                    .withLatLng(MLLatLng(loc.lat, loc.lng))
                                    .withCircleRadius(10f)
                                    .withCircleColor("#1D6FD8")       // azul primario
                                    .withCircleStrokeWidth(2.5f)
                                    .withCircleStrokeColor("#FFFFFF") // borde blanco
                                    .withCircleOpacity(0.95f)
                            )
                            // Punto de pulso exterior semitransparente
                            map.addCircle(
                                MLCircleOptions()
                                    .withLatLng(MLLatLng(loc.lat, loc.lng))
                                    .withCircleRadius(20f)
                                    .withCircleColor("#1D6FD8")
                                    .withCircleOpacity(0.25f)
                                    .withCircleStrokeWidth(0f)
                            )
                        }
                    }
                    if (polyline.size >= 2) {
                        map.addPolyline(
                            MLPolylineOptions()
                                .addAll(polyline.map { MLLatLng(it.lat, it.lng) })
                                .color(android.graphics.Color.parseColor("#2563EB"))
                                .width(3f).alpha(0.85f)
                        )
                    }
                    // Fit bounds la primera vez que llegan stops con GPS
                    if (!didInitialFit && stops.any { it.latLng.lat != 0.0 }) {
                        didInitialFit = true
                        // Si ya tenemos localización del usuario, incluirla en el bounds
                        val allPts = stops.filter { it.latLng.lat != 0.0 && it.latLng.lng != 0.0 }
                        if (allPts.size >= 2) {
                            val latMin = allPts.minOf { it.latLng.lat }
                            val latMax = allPts.maxOf { it.latLng.lat }
                            val lngMin = allPts.minOf { it.latLng.lng }
                            val lngMax = allPts.maxOf { it.latLng.lng }
                            val bounds = org.maplibre.android.geometry.LatLngBounds.from(
                                latMax, lngMax, latMin, lngMin)
                            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 800)
                        } else if (allPts.size == 1) {
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                MLLatLng(allPts[0].latLng.lat, allPts[0].latLng.lng), 14.0), 600)
                        }
                    }
                }
            }
        )
    }

        private fun addStopMarkers(
        map: MapLibreMap,
        stops: List<StopMapMarker>,
        opts: com.pabl3st.rutapp.core.map.MarkerOptions,
    ) {
        stops
            .filter { it.latLng.lat != 0.0 && it.latLng.lng != 0.0 }
            .forEach { stop ->
                val label = when {
                    opts.showExternalId && stop.externalId != null ->
                        "${stop.externalId} · ${stop.name}"
                    opts.style == MarkerStyle.NUMBERED ->
                        "${stop.orderIndex + 1}. ${stop.name}"
                    else -> stop.name
                }

                map.addMarker(
                    MLMarkerOptions()
                        .position(MLLatLng(stop.latLng.lat, stop.latLng.lng))
                        .title(label)
                        .snippet(stop.uid)
                )
            }
    }

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
        val gMapsUri    = Uri.parse("google.navigation:q=${destination.lat},${destination.lng}&mode=$modeParam")
        val gMapsIntent = Intent(Intent.ACTION_VIEW, gMapsUri).apply { setPackage("com.google.android.apps.maps") }

        when {
            gMapsIntent.resolveActivity(context.packageManager) != null ->
                context.startActivity(gMapsIntent)
            else -> {
                val geoUri = Uri.parse(
                    "geo:${destination.lat},${destination.lng}?q=${destination.lat},${destination.lng}" +
                    (label?.let { "(${Uri.encode(it)})" } ?: "")
                )
                val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
                if (geoIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(geoIntent)
                } else {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?daddr=${destination.lat},${destination.lng}")))
                }
            }
        }
    }

    override suspend fun calculateRoute(
        origin: MapLatLng,
        stops: List<MapLatLng>,
        options: RouteOptions,
    ): MapRoute? = withContext(Dispatchers.IO) {
        if (stops.isEmpty()) return@withContext null
        runCatching {
            // OSRM public API — gratuito, sin API key
            // Formato: /route/v1/{profile}/{lon,lat};{lon,lat}?overview=full&geometries=geojson
            val profile = when (options.mode) {
                RouteMode.WALKING  -> "foot"
                RouteMode.CYCLING  -> "bike"
                else               -> "car"
            }
            val coords = (listOf(origin) + stops)
                .joinToString(";") { "${it.lng},${it.lat}" }
            val url = "https://router.project-osrm.org/route/v1/$profile/$coords" +
                      "?overview=full&geometries=geojson&steps=false"

            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "RutasApp Android")
            conn.connectTimeout = 8_000
            conn.readTimeout    = 8_000

            val body = conn.inputStream.bufferedReader().readText()
            val json = org.json.JSONObject(body)

            if (json.optString("code") != "Ok") return@runCatching null

            val route    = json.getJSONArray("routes").getJSONObject(0)
            val distance = route.getDouble("distance")
            val duration = route.getDouble("duration").toInt()
            val geometry = route.getJSONObject("geometry")
            val coords2  = geometry.getJSONArray("coordinates")

            val points = (0 until coords2.length()).map { i ->
                val pt = coords2.getJSONArray(i)
                MapLatLng(pt.getDouble(1), pt.getDouble(0))   // GeoJSON: [lng, lat]
            }

            MapRoute(points = points, distanceMeters = distance, durationSeconds = duration)
        }.getOrNull()
    }

    override suspend fun optimizeStopOrder(
        origin: MapLatLng,
        stops: List<StopMapMarker>,
        options: RouteOptions,
    ): List<StopMapMarker> {
        if (stops.size <= 1) return stops
        val remaining = stops.toMutableList()
        val ordered   = mutableListOf<StopMapMarker>()
        var current   = origin
        while (remaining.isNotEmpty()) {
            val nearest = remaining.minByOrNull { s ->
                val dx = s.latLng.lat - current.lat
                val dy = s.latLng.lng - current.lng
                dx * dx + dy * dy
            } ?: break
            ordered.add(nearest)
            remaining.remove(nearest)
            current = nearest.latLng
        }
        return ordered
    }

    override suspend fun geocode(address: String): MapLatLng? = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = Uri.encode(address)
            val conn = java.net.URL(
                "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1"
            ).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "RutasApp Android")
            val json = org.json.JSONArray(conn.inputStream.bufferedReader().readText())
            if (json.length() == 0) return@runCatching null
            val obj = json.getJSONObject(0)
            MapLatLng(obj.getDouble("lat"), obj.getDouble("lon"))
        }.getOrNull()
    }

    override suspend fun reverseGeocode(location: MapLatLng): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = java.net.URL(
                "https://nominatim.openstreetmap.org/reverse?lat=${location.lat}&lon=${location.lng}&format=json"
            ).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "RutasApp Android")
            org.json.JSONObject(conn.inputStream.bufferedReader().readText())
                .optString("display_name").ifEmpty { null }
        }.getOrNull()
    }

    override suspend fun downloadOfflineRegion(
        name: String, bounds: Pair<MapLatLng, MapLatLng>, minZoom: Float, maxZoom: Float,
    ) = false  // TODO S07

    override suspend fun deleteOfflineRegion(name: String) = false
    override suspend fun getOfflineRegions(): List<String> = emptyList()
}

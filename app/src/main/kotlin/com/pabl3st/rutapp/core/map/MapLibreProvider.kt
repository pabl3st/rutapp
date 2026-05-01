package com.pabl3st.rutapp.core.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MLLatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
// Alias explícito para evitar conflicto con nuestra MarkerOptions
import org.maplibre.android.annotations.MarkerOptions as MLMarkerOptions

/**
 * MapLibre Provider — OpenStreetMap
 * Sin API key, sin cuenta, sin límites, offline-capable.
 */
class MapLibreProvider(private val context: Context) : MapProvider {

    override val type             = MapProviderType.MAPLIBRE
    override val supportsOffline  = true
    override val supportsTraffic  = false
    override val supportsRouting  = false
    override val supportsSatellite = true

    private fun styleUrl(style: MapStyle, darkMode: Boolean): String = when (style) {
        MapStyle.DARK       -> "https://tiles.openfreemap.org/styles/dark"
        MapStyle.LIGHT,
        MapStyle.STANDARD   -> if (darkMode) "https://tiles.openfreemap.org/styles/dark"
                               else "https://tiles.openfreemap.org/styles/bright"
        MapStyle.SATELLITE,
        MapStyle.HYBRID     -> "https://demotiles.maplibre.org/satellite-style.json"
        MapStyle.TERRAIN    -> "https://demotiles.maplibre.org/style.json"
        MapStyle.TRAFFIC,
        MapStyle.NAVIGATION -> "https://tiles.openfreemap.org/styles/bright"
    }

    // Convierte Long de color (0xFF2563EB) a string hex "#2563eb"
    private fun Long.toHexColor(): String = "#%06x".format(this and 0xFFFFFF)

    private fun markerColorHex(status: String, opts: com.pabl3st.rutapp.core.map.MarkerOptions): String = when (status) {
        "done"     -> opts.doneColor.toHexColor()
        "visiting" -> opts.visitingColor.toHexColor()
        "skipped"  -> opts.skippedColor.toHexColor()
        else       -> opts.pendingColor.toHexColor()
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
        val ctx     = LocalContext.current
        val isDark  = androidx.compose.foundation.isSystemInDarkTheme()

        LaunchedEffect(Unit) { MapLibre.getInstance(ctx) }

        var mlMap by remember { mutableStateOf<MapLibreMap?>(null) }

        AndroidView(
            modifier = modifier,
            factory  = { factoryCtx ->
                MapView(factoryCtx).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        mlMap = map

                        map.setStyle(styleUrl(config.style, isDark))

                        map.uiSettings.apply {
                            isCompassEnabled          = config.layers.showCompass
                            // isZoomControlsEnabled no existe en MapLibre 11.x — zoom UI se gestiona vía isZoomGesturesEnabled
                            isRotateGesturesEnabled   = config.camera.rotationEnabled
                            isTiltGesturesEnabled     = config.camera.tiltEnabled
                            isScrollGesturesEnabled   = config.camera.scrollEnabled
                            isZoomGesturesEnabled     = config.camera.zoomEnabled
                        }

                        map.setMinZoomPreference(config.camera.minZoom.toDouble())
                        map.setMaxZoomPreference(config.camera.maxZoom.toDouble())

                        map.addOnMapClickListener { latLng ->
                            onMapClick(MapLatLng(latLng.latitude, latLng.longitude))
                            true
                        }

                        map.addOnCameraIdleListener {
                            val pos = map.cameraPosition
                            val target = pos.target  // puede ser null
                            if (target != null) {
                                onCameraIdle(
                                    MapLatLng(target.latitude, target.longitude),
                                    pos.zoom.toFloat()
                                )
                            }
                        }

                        map.setOnMarkerClickListener { marker ->
                            val uid = marker.snippet ?: return@setOnMarkerClickListener false
                            onStopClick(uid)
                            true
                        }

                        // Centrar cámara en usuario o primer stop pendiente
                        val target = userLocation?.let { MLLatLng(it.lat, it.lng) }
                            ?: stops.firstOrNull { it.status != "done" && it.latLng.lat != 0.0 }
                                ?.let { MLLatLng(it.latLng.lat, it.latLng.lng) }

                        if (target != null) {
                            map.moveCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(target)
                                        .zoom(config.camera.initialZoom.toDouble())
                                        .build()
                                )
                            )
                        }

                        addStopMarkers(map, stops, config.markers)
                    }
                }
            },
            update = { _ ->
                mlMap?.let { map ->
                    map.clear()
                    addStopMarkers(map, stops, config.markers)
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
    ): MapRoute? = null   // TODO S07 con OSRM

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

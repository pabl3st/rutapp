package com.pabl3st.rutapp.core.map

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ── Placeholder para proveedores no configurados ──────────────
@Composable
fun NotConfiguredPlaceholder(
    providerName: String,
    requiresKey: Boolean,
    keyHint: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Default.Map, null, Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))
            Text("$providerName no configurado",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface)
            if (requiresKey) {
                Spacer(Modifier.height(8.dp))
                Text(keyHint,
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("Configura la API key en Perfil → Ajustes → Mapas",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

// ── Factory — crea el proveedor correcto según config ─────────
object MapProviderFactory {

    fun create(context: Context, config: MapConfig): MapProvider = when (config.provider) {
        MapProviderType.MAPLIBRE    -> MapLibreProvider(context)
        MapProviderType.GOOGLE_MAPS -> GoogleMapsProvider()
        MapProviderType.MAPBOX      -> MapboxProvider()
        MapProviderType.HERE        -> HereMapsProvider()
    }

    // Configuración por defecto — MapLibre sin key
    fun defaultConfig(): MapConfig = MapConfig(
        provider = MapProviderType.MAPLIBRE,
        style    = MapStyle.STANDARD,
        darkModeFollowSystem = true,
    )

    // Configuración recomendada para field sales
    fun fieldSalesConfig(providerType: MapProviderType = MapProviderType.MAPLIBRE): MapConfig = MapConfig(
        provider = providerType,
        style    = MapStyle.STANDARD,
        layers   = MapLayerOptions(
            showTraffic   = providerType != MapProviderType.MAPLIBRE,
            showPOIs      = true,
            showCompass   = true,
            showScaleBar  = true,
        ),
        camera   = CameraOptions(
            initialZoom     = 13f,
            minZoom         = 5f,
            maxZoom         = 20f,
            tiltEnabled     = true,
            rotationEnabled = true,
        ),
        markers  = MarkerOptions(
            style           = MarkerStyle.NUMBERED,
            showLabel       = true,
            showExternalId  = true,
        ),
        cluster  = ClusterOptions(
            enabled = true,
            minZoom = 10f,
            maxZoom = 14f,
        ),
        route    = RouteOptions(
            mode          = RouteMode.DRIVING,
            optimizeOrder = false,   // activar en S07 con TSP
            showPolyline  = true,
        ),
        darkModeFollowSystem = true,
    )
}

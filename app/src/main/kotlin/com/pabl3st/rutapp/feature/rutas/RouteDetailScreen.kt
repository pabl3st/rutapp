@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.rutas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.StopTagConfig
import com.pabl3st.rutapp.data.local.entity.evaluateTag
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.GpsOff

@Composable
fun RouteDetailScreen(
    routeUid: String,
    onBack: () -> Unit,
    onNavigateToMap: (String) -> Unit = {},
    onStopClick: (String) -> Unit = {},
    onAddStop: (String) -> Unit = {},
    vm: RouteDetailViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.error) {
        ui.error?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.route?.name ?: "Ruta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToMap(routeUid) }) {
                        Icon(Icons.Default.Map, contentDescription = "Ver en mapa")
                    }
                    ui.route?.let { route ->
                        StatusChip(
                            status   = route.status,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddStop(routeUid) }) {
                Icon(Icons.Default.AddLocation, contentDescription = "Añadir parada")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // ── Selector de modo de ordenación ─────────────────
            if (ui.stops.isNotEmpty()) {
                SortModeSelector(
                    current   = ui.sortMode,
                    onChange  = { vm.setSortMode(it) },
                    onSave    = { vm.saveCurrentOrder() },
                    isSaving  = ui.isReordering,
                    modifier  = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                ui.stops.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Place, null, Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(12.dp))
                        Text("Sin paradas", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Pulsa + para añadir la primera",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ui.stops, key = { it.uid }) { stop ->
                        StopCard(
                            stop         = stop,
                            stopTags     = ui.stopTags,
                            kpiValues    = ui.kpiByStop[stop.uid] ?: emptyMap(),
                            onOpenVisita = { onStopClick(stop.uid) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortModeSelector(
    current:  StopSortMode,
    onChange: (StopSortMode) -> Unit,
    onSave:   () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortModeChip(
            label    = "Manual",
            icon     = Icons.Default.DragHandle,
            selected = current == StopSortMode.MANUAL,
            onClick  = { onChange(StopSortMode.MANUAL) },
            modifier = Modifier.weight(1f),
        )
        SortModeChip(
            label    = "Por GPS",
            icon     = Icons.Default.GpsFixed,
            selected = current == StopSortMode.GPS,
            onClick  = { onChange(StopSortMode.GPS) },
            modifier = Modifier.weight(1f),
        )
        SortModeChip(
            label    = "Óptimo",
            icon     = Icons.Default.Route,
            selected = current == StopSortMode.GREEDY,
            onClick  = { onChange(StopSortMode.GREEDY) },
            modifier = Modifier.weight(1f),
        )
        if (current != StopSortMode.MANUAL) {
            IconButton(
                onClick  = onSave,
                enabled  = !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = "Guardar orden",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SortModeChip(
    label:    String,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        },
        modifier = modifier,
    )
}

@Composable
private fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (color, icon, label) = when (status) {
        "active"    -> Triple(MaterialTheme.colorScheme.primary,          Icons.Default.PlayCircle, "Activa")
        "done"      -> Triple(MaterialTheme.colorScheme.secondary,        Icons.Default.CheckCircle, "Completada")
        "cancelled" -> Triple(MaterialTheme.colorScheme.error,            Icons.Default.Cancel, "Cancelada")
        else        -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Schedule, "Pendiente")
    }
    SuggestionChip(
        onClick  = {},
        modifier = modifier,
        label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon     = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors   = SuggestionChipDefaults.suggestionChipColors(
            labelColor       = color,
            iconContentColor = color,
        ),
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StopCard(
    stop:        StopEntity,
    stopTags:    List<StopTagConfig> = emptyList(),
    kpiValues:   Map<String, String> = emptyMap(),
    onOpenVisita: () -> Unit = {},
) {
    val isDone     = stop.status == "done"
    val isVisiting = stop.status == "visiting"
    val noGps      = stop.lat == null || stop.lng == null

    // Evaluar tags configurados por el owner
    val activeTags = stopTags.filter { evaluateTag(it, stop, kpiValues) }

    // Tag de sistema: SIN GPS
    val systemTags = buildList {
        if (noGps) add(Triple("SIN GPS", "#fee2e2", "#dc2626"))
    }

    val cardColor = when {
        isVisiting -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        isDone     -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else       -> CardDefaults.cardColors()
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = cardColor) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Número de orden
                Surface(
                    shape    = MaterialTheme.shapes.small,
                    color    = when {
                        isDone     -> MaterialTheme.colorScheme.tertiaryContainer
                        isVisiting -> MaterialTheme.colorScheme.primaryContainer
                        else       -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text  = "${stop.orderIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isDone     -> MaterialTheme.colorScheme.onTertiaryContainer
                                isVisiting -> MaterialTheme.colorScheme.onPrimaryContainer
                                else       -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    stop.externalId?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    }
                    Text(
                        text  = stop.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                    )
                    stop.address?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (!isDone) {
                    IconButton(onClick = onOpenVisita) {
                        Icon(Icons.Default.Edit, "Registrar visita",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(12.dp))
                }
            }

            // ── Tags ──────────────────────────────────────────
            if (activeTags.isNotEmpty() || systemTags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp),
                ) {
                    systemTags.forEach { (name, bg, fg) ->
                        StopTagChip(name, bg, fg)
                    }
                    activeTags.forEach { tag ->
                        StopTagChip(tag.name, tag.colorHex, tag.textColorHex)
                    }
                }
            }
        }
    }
}

@Composable
private fun StopTagChip(name: String, colorHex: String, textColorHex: String) {
    val bg  = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color.LightGray)
    val fg  = runCatching { Color(android.graphics.Color.parseColor(textColorHex)) }.getOrDefault(Color.Black)
    Surface(shape = MaterialTheme.shapes.extraSmall, color = bg) {
        Text(
            text     = name,
            style    = MaterialTheme.typography.labelSmall,
            color    = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}



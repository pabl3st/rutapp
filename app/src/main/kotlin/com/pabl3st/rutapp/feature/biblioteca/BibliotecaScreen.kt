@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.biblioteca

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
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.StopTagConfig
import com.pabl3st.rutapp.data.local.entity.evaluateTag
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.graphics.Color

@Composable
fun BibliotecaScreen(
    onBack: () -> Unit = {},
    onStopClick: (String) -> Unit = {},
    vm: BibliotecaViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca de paradas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // ── Buscador ──────────────────────────────────────
            OutlinedTextField(
                value         = ui.query,
                onValueChange = vm::onQueryChange,
                placeholder   = { Text("Buscar parada…") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = {
                    if (ui.query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Limpiar")
                        }
                    }
                },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // ── Tabs ──────────────────────────────────────────
            TabRow(selectedTabIndex = ui.tab.ordinal) {
                BibliotecaTab.entries.forEach { tab ->
                    Tab(
                        selected = ui.tab == tab,
                        onClick  = { vm.onTabChange(tab) },
                        text     = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        icon     = { Icon(tab.icon, null, Modifier.size(16.dp)) },
                    )
                }
            }

            if (ui.filteredStops.isNotEmpty()) {
                Text(
                    text     = "${ui.filteredStops.size} paradas",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                ui.filteredStops.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when (ui.tab) {
                                BibliotecaTab.ALL    -> if (ui.query.isEmpty()) "Sin paradas" else "Sin resultados"
                                BibliotecaTab.NO_GPS -> "Todas las paradas tienen GPS \u2713"
                                BibliotecaTab.ORPHAN -> "Todas las paradas están asignadas \u2713"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ui.filteredStops, key = { it.uid }) { stop ->
                        BibliotecaStopCard(stop = stop, stopTags = ui.stopTags, onClick = { onStopClick(stop.uid) })
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BibliotecaStopCard(
    stop:     StopEntity,
    stopTags: List<StopTagConfig> = emptyList(),
    onClick:  () -> Unit,
) {
    // Biblioteca: solo tags estáticos (ALWAYS, STATUS, DAYS_SINCE_VISIT)
    // No se pasan kpiValues — los tags de KPI no aplican aquí
    val activeTags = stopTags.filter { evaluateTag(it, stop, emptyMap()) }

    val hasGps    = stop.lat != null && stop.lng != null
    val isOrphan  = false // ya filtrado por la pestaña — info visual extra

    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Indicador GPS
                Surface(
                    shape    = MaterialTheme.shapes.small,
                    color    = if (hasGps) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector        = if (hasGps) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp),
                            tint               = if (hasGps) MaterialTheme.colorScheme.onPrimaryContainer
                                                 else MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    stop.externalId?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Text(stop.name, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    stop.address?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                // Prioridad
                val pColor = when (stop.priority) {
                    1    -> MaterialTheme.colorScheme.error
                    2    -> MaterialTheme.colorScheme.tertiary
                    3    -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(shape = MaterialTheme.shapes.extraSmall, color = pColor.copy(alpha = 0.15f)) {
                    Text("P${stop.priority}", style = MaterialTheme.typography.labelSmall,
                        color = pColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Icon(Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }

            // Tags estáticos — solo si hay alguno activo
            if (activeTags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp),
                ) {
                    activeTags.forEach { tag ->
                        val bg = runCatching {
                            Color(android.graphics.Color.parseColor(tag.colorHex))
                        }.getOrDefault(Color.LightGray)
                        val fg = runCatching {
                            Color(android.graphics.Color.parseColor(tag.textColorHex))
                        }.getOrDefault(Color.Black)
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = bg) {
                            Text(tag.name, style = MaterialTheme.typography.labelSmall,
                                color = fg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}


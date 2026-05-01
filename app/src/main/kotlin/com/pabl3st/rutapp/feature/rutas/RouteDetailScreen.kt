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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.data.local.entity.StopEntity

@Composable
fun RouteDetailScreen(
    routeUid: String,
    onBack: () -> Unit,
    onNavigateToMap: (String) -> Unit = {},
    vm: RouteDetailViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

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
                        val statusColor = when (route.status) {
                            "active" -> MaterialTheme.colorScheme.primary
                            "done"   -> MaterialTheme.colorScheme.tertiary
                            else     -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        SuggestionChip(
                            onClick  = {},
                            label    = { Text(route.status, style = MaterialTheme.typography.labelSmall) },
                            colors   = SuggestionChipDefaults.suggestionChipColors(labelColor = statusColor),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::onShowAddStopDialog) {
                Icon(Icons.Default.AddLocation, contentDescription = "Añadir parada")
            }
        }
    ) { padding ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            ui.stops.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
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
                modifier            = Modifier.padding(padding),
            ) {
                items(ui.stops, key = { it.uid }) { stop ->
                    StopCard(
                        stop           = stop,
                        onMarkVisited  = { vm.markStopVisited(stop.uid) },
                    )
                }
            }
        }
    }

    // ── Diálogo añadir stop ────────────────────────────────────
    if (ui.showAddStopDialog) {
        AlertDialog(
            onDismissRequest = vm::onDismissAddStopDialog,
            title = { Text("Nueva parada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value         = ui.newStopName,
                        onValueChange = vm::onNewStopNameChange,
                        label         = { Text("Nombre del cliente / punto") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        isError       = ui.error != null,
                        supportingText = ui.error?.let { err ->
                            { Text(err, color = MaterialTheme.colorScheme.error) }
                        },
                    )
                    OutlinedTextField(
                        value         = ui.newStopExternalId,
                        onValueChange = vm::onNewStopExternalIdChange,
                        label         = { Text("Código cliente (opcional)") },
                        placeholder   = { Text("Ej: LCC00237", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value         = ui.newStopAddress,
                        onValueChange = vm::onNewStopAddressChange,
                        label         = { Text("Dirección (opcional)") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton  = { TextButton(onClick = vm::addStop) { Text("Añadir") } },
            dismissButton  = { TextButton(onClick = vm::onDismissAddStopDialog) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun StopCard(stop: StopEntity, onMarkVisited: () -> Unit) {
    val isDone = stop.status == "done"
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Número de orden
            Surface(
                shape    = MaterialTheme.shapes.small,
                color    = if (isDone) MaterialTheme.colorScheme.tertiaryContainer
                           else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text  = "${stop.orderIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDone) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                // Mostrar external_id si existe (ej: LCC00237)
                stop.externalId?.let { extId ->
                    Text(
                        text  = extId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
                Text(
                    text  = stop.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                )
                stop.address?.let { addr ->
                    Text(addr, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (!isDone) {
                IconButton(onClick = onMarkVisited) {
                    Icon(Icons.Default.CheckCircleOutline,
                        contentDescription = "Marcar visitado",
                        tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(12.dp))
            }
        }
    }
}

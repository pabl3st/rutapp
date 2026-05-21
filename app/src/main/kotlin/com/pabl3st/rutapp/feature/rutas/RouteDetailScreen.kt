@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.rutas

import com.pabl3st.rutapp.core.ui.theme.RouteStatusTokens
import com.pabl3st.rutapp.core.ui.theme.StopStatusTokens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.data.local.entity.StopEntity

@Composable
fun RouteDetailScreen(
    routeUid: String,
    onBack: () -> Unit,
    onNavigateToMap: (String) -> Unit = {},
    onStopClick:     (String) -> Unit = {},
    onAddStop:       (String) -> Unit = {},
    onEditStop:      (String) -> Unit = {},
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
    LaunchedEffect(ui.snackbar) {
        ui.snackbar?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            vm.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.semantics { testTag = "route-detail-screen" },
        topBar = {
            TopAppBar(
                title = { Text(ui.route?.name ?: "Ruta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Reasignar ruta — solo roles con permiso
                    if (ui.canReassign) {
                        IconButton(onClick = vm::onShowReassignDialog) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Reasignar ruta")
                        }
                    }
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
            if (ui.canEditStops) {
                FloatingActionButton(onClick = { onAddStop(routeUid) }) {
                    Icon(Icons.Default.AddLocation, contentDescription = "Añadir parada")
                }
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

            // ── Selector de fecha si hay múltiples días ───────
            if (ui.availableDates.size > 1) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(ui.availableDates) { date ->
                        val today   = java.time.LocalDate.now().toString()
                        val label   = if (date == today) "Hoy" else
                            runCatching {
                                java.time.LocalDate.parse(date).format(
                                    java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("es")))
                            }.getOrDefault(date)
                        FilterChip(
                            selected = date == ui.selectedDate,
                            onClick  = { vm.onDateSelected(date) },
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                           verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(
                            "Sincronizando ruta...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                    itemsIndexed(ui.stops, key = { _, s -> s.uid }) { idx, stop ->
                        StopCard(
                            modifier     = Modifier.semantics { testTag = "stop-card-$idx" },
                            stop         = stop,
                            onOpenVisita = { onStopClick(stop.uid) },
                            onEdit       = if (ui.canEditStops) ({ onEditStop(stop.uid) }) else null,
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo reasignar ruta ────────────────────────────────
    if (ui.showReassignDialog) {
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = vm::onDismissReassignDialog,
            title = { Text("Reasignar ruta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Selecciona el nuevo responsable de esta ruta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (ui.loadingUsers) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Cargando equipo…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (ui.assignableUsers.isNotEmpty()) {
                        val selectedName = ui.assignableUsers
                            .firstOrNull { it.userId == ui.selectedAssigneeId }
                            ?.let { "${it.displayName} (${it.role})" }
                            ?: "Seleccionar…"
                        ExposedDropdownMenuBox(
                            expanded         = expanded,
                            onExpandedChange = { expanded = !expanded },
                        ) {
                            OutlinedTextField(
                                value         = selectedName,
                                onValueChange = {},
                                readOnly      = true,
                                label         = { Text("Asignar a") },
                                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier      = Modifier.fillMaxWidth().menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded         = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                ui.assignableUsers.forEach { user ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(user.displayName,
                                                    style = MaterialTheme.typography.bodyMedium)
                                                Text("${user.role} · @${user.username}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = { vm.onSelectAssignee(user.userId); expanded = false },
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "No hay usuarios disponibles para reasignar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = vm::confirmReassign,
                    enabled = ui.selectedAssigneeId != null && !ui.isReassigning,
                ) {
                    if (ui.isReassigning) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Reasignar")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::onDismissReassignDialog) { Text("Cancelar") }
            },
        )
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
    val st = RouteStatusTokens.of(status)
    val color = st.color; val icon = st.icon; val label = st.label
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
private fun StopCard(
    stop:         StopEntity,
    onOpenVisita: () -> Unit    = {},
    onEdit:       (() -> Unit)? = null,
    modifier:     Modifier      = Modifier,
) {
    val isDone = stop.status == "done"
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                // Cabecera: externalId + chip de fecha si hay múltiples fechas
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    stop.externalId?.let { extId ->
                        Text(
                            text  = extId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                    }
                    stop.dateAssigned?.let { date ->
                        val today = java.time.LocalDate.now().toString()
                        val label = if (date == today) "Hoy" else
                            runCatching {
                                java.time.LocalDate.parse(date).format(
                                    java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("es")))
                            }.getOrDefault(date)
                        val isToday = date == today
                        Surface(shape = MaterialTheme.shapes.extraSmall,
                            color = if (isToday) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant) {
                            Text(label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
                Text(
                    text  = stop.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                )
                stop.address?.let { addr ->
                    Text(addr, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                // Resultado última visita + fecha
                stop.visitResult?.let { result ->
                    val (label, color) = when (result) {
                        "contactado" -> "Contactado"  to androidx.compose.ui.graphics.Color(0xFF1D9E75)
                        "no_estaba"  -> "No estaba"   to MaterialTheme.colorScheme.error
                        "volvemos"   -> "Volvemos"    to MaterialTheme.colorScheme.tertiary
                        "rechazado"  -> "Rechazado"   to MaterialTheme.colorScheme.error
                        else         -> result        to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier              = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(Icons.Default.History, null, Modifier.size(10.dp), tint = color)
                        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                        stop.visitedAt?.take(10)?.let { date ->
                            Text("· $date", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Próxima acción
                stop.nextAction?.takeIf { it.isNotBlank() }?.let { action ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier              = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(Icons.Default.NextPlan, null, Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Text(action, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            // Badge prioridad + estado PDV
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (stop.priority in 1..3) {
                    val pColor = when (stop.priority) {
                        1 -> MaterialTheme.colorScheme.error
                        2 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Surface(shape = MaterialTheme.shapes.extraSmall, color = pColor.copy(alpha = 0.15f)) {
                        Text("P${stop.priority}",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = pColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                }
                if (stop.pdvInactive) {
                    Icon(Icons.Default.Block, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error)
                } else if (!stop.pdvOpen) {
                    Icon(Icons.Default.StoreMallDirectory, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
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
                if (onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Tune, "Editar datos del PDV",
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

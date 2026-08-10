@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.rutas

import com.pabl3st.rutapp.core.ui.theme.RouteStatusTokens
import com.pabl3st.rutapp.core.ui.theme.StopStatusTokens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.pabl3st.rutapp.data.local.entity.StopVisitEntity

@Composable
fun RouteDetailScreen(
    routeUid: String,
    onBack: () -> Unit,
    onNavigateToMap: (String) -> Unit = {},
    /** uid de la parada + fecha de la ocasion seleccionada en los chips. */
    onStopClick:     (String, String?) -> Unit = { _, _ -> },
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
                title = {
                    Column {
                        Text(ui.route?.name ?: "Ruta",
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        ui.routeOwnerName?.let { name ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.Person, null, Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Text(name, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
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
                        IconButton(onClick = vm::onShowHistory) {
                            Icon(Icons.Default.History, contentDescription = "Historial de asignación")
                        }
                    }
                    if (ui.stops.isNotEmpty() && ui.canEditStops) {
                        var showClearConfirm by remember { mutableStateOf(false) }
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Default.DeleteSweep, "Vaciar ruta",
                                tint = MaterialTheme.colorScheme.error)
                        }
                        if (showClearConfirm) {
                            AlertDialog(
                                onDismissRequest = { showClearConfirm = false },
                                title = { Text("Vaciar ruta") },
                                text  = { Text("Se quitarán todas las paradas. Los datos de visita no se pierden.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = { vm.clearRouteStops(); showClearConfirm = false },
                                        colors  = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Vaciar") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showClearConfirm = false }) { Text("Cancelar") }
                                },
                            )
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
            // ── Jornada: iniciar / pausar / finalizar ──────────
            // Mismo componente que en Hoy, mismo estado (Room es la fuente
            // de verdad), asi que los contadores coinciden y da igual desde
            // que pantalla se inicie, pause o finalice.
            // Se le pasa la fecha de la ruta: una ruta rellenada a posteriori
            // cierra su jornada en SU dia, no en el de hoy.
            com.pabl3st.rutapp.feature.home.JornadaBar(
                routeUid  = routeUid,
                // La ocasion elegida en los chips manda sobre dateAssigned.
                // Con PS02 (11/8 y 21/8) cerrar la jornada siempre en
                // dateAssigned dejaba el 21/8 imposible de poner en verde:
                // el calendario pinta por clave "routeUid|dateStr".
                routeDate = ui.selectedDate ?: ui.route?.dateAssigned,
            )

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
                        Text("Pulsa + para añadir paradas de tu biblioteca",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        if (ui.canEditStops) {
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { onAddStop(routeUid) }) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Añadir paradas")
                            }
                        }
                    }
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(ui.stops, key = { _, s -> s.uid }) { idx, stop ->
                        StopCard(
                            modifier   = Modifier.semantics { testTag = "stop-card-$idx" },
                            stop       = stop,
                            visit      = ui.visitsByStop[stop.uid],
                            onRemove   = if (ui.canEditStops) ({ vm.removeStop(stop.uid) }) else null,
                            onOpenForm = { onStopClick(stop.uid, ui.selectedDate) },
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
                    // Motivo opcional — queda registrado en el historial
                    OutlinedTextField(
                        value         = ui.reassignReason,
                        onValueChange = vm::onReassignReasonChange,
                        label         = { Text("Motivo (opcional)") },
                        placeholder   = { Text("Baja, vacaciones, reequilibrio…") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )
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

    // ── Diálogo de historial de reasignación ──────────────────
    if (ui.showHistory) {
        AlertDialog(
            onDismissRequest = vm::onDismissHistory,
            icon  = { Icon(Icons.Default.History, null) },
            title = { Text("Historial de asignación") },
            text  = {
                when {
                    ui.loadingHistory -> Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(Modifier.size(28.dp)) }
                    ui.history.isEmpty() -> Text(
                        "Esta ruta no se ha reasignado nunca.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    else -> Column(
                        Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ui.history.forEach { h ->
                            Column {
                                Text(
                                    "${h.fromUserName ?: "Sin asignar"}  →  ${h.toUserName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    "Por ${h.assignedByName} · ${h.createdAt.take(10)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                h.reason?.takeIf { it.isNotBlank() }?.let { r ->
                                    Text(
                                        "Motivo: $r",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = vm::onDismissHistory) { Text("Cerrar") }
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
    stop:       StopEntity,
    visit:      StopVisitEntity? = null,
    onRemove:   (() -> Unit)? = null,
    onOpenForm: (() -> Unit)? = null,
    modifier:   Modifier      = Modifier,
) {
    // Modelo C: cuando hay visita de la fecha seleccionada, ella manda.
    // Cuando no la hay (rutas legacy o ruta sin scheduledDates), nos quedamos
    // con los campos espejo del stop (lastVisitMirror).
    val effectiveStatus      = visit?.status      ?: stop.status
    val effectiveVisitedAt   = visit?.visitedAt   ?: stop.visitedAt
    val effectiveVisitResult = visit?.visitResult ?: stop.visitResult
    val effectiveNextAction  = visit?.nextAction  ?: stop.nextAction
    val effectiveVisitDate   = visit?.visitDate   ?: stop.dateAssigned
    val isDone = effectiveStatus == "done"
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
                    effectiveVisitDate?.let { date ->
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
                effectiveVisitResult?.let { result ->
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
                        // La fecha que se pinta es la de la OCASION (visitDate),
                        // no la del momento real en que se relleno el formulario.
                        // Si el 10/8 rellenas la parada asignada al 11/8, aqui
                        // debe leerse 11/8: es la fecha del calendario a la que
                        // pertenece el trabajo. effectiveVisitedAt guarda el
                        // instante real y sigue disponible para auditoria.
                        effectiveVisitDate?.take(10)?.let { date ->
                            Text("· $date", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Próxima acción
                effectiveNextAction?.takeIf { it.isNotBlank() }?.let { action ->
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
            // Abrir formulario de visita directamente desde la lista de paradas
            if (onOpenForm != null) {
                IconButton(
                    onClick  = onOpenForm,
                    modifier = Modifier.size(40.dp).semantics { testTag = "stop-form-${stop.uid}" },
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = "Abrir formulario de visita",
                        modifier = Modifier.size(20.dp),
                        tint     = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                   else MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.RemoveCircleOutline, "Quitar de ruta",
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

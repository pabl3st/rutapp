@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.rutas

import com.pabl3st.rutapp.core.ui.theme.RouteStatusTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RutasScreen(
    onRouteClick: (String) -> Unit,
    onBack: () -> Unit,
    onImport: () -> Unit = {},
    vm: RutasViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar errores de reasignación masiva (y otros) vía snackbar.
    // El diálogo de crear ruta gestiona su propio error inline, así que
    // solo se muestra el snackbar cuando no hay diálogo de creación abierto.
    LaunchedEffect(ui.error) {
        val err = ui.error
        if (err != null && !ui.showCreateDialog) {
            snackbarHostState.showSnackbar(err)
            vm.clearError()
        }
    }

    // Resultado del botón "Forzar sincronización" — mensaje persistente
    // hasta que el usuario lo descarte (duración Long).
    LaunchedEffect(ui.forceSyncResult) {
        val msg = ui.forceSyncResult
        if (msg != null) {
            snackbarHostState.showSnackbar(
                message  = msg,
                duration = androidx.compose.material3.SnackbarDuration.Long,
            )
            vm.clearForceSyncResult()
        }
    }

    // Salir del modo selección con el botón atrás del sistema
    androidx.activity.compose.BackHandler(enabled = ui.selectionMode) {
        vm.exitSelectionMode()
    }

    Scaffold(
        topBar = {
            if (ui.selectionMode) {
                TopAppBar(
                    title = { Text("${ui.selectedRouteUids.size} seleccionada(s)") },
                    navigationIcon = {
                        IconButton(onClick = vm::exitSelectionMode) {
                            Icon(Icons.Default.Close, contentDescription = "Salir de selección")
                        }
                    },
                    actions = {
                        val allSelected = ui.routes.isNotEmpty() &&
                            ui.selectedRouteUids.size == ui.routes.size
                        IconButton(onClick = {
                            if (allSelected) vm.clearSelection() else vm.selectAllRoutes()
                        }) {
                            Icon(
                                if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deseleccionar todo"
                                                     else "Seleccionar todo",
                            )
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text("Rutas") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        if (ui.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).padding(end = 4.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        // Forzar sync manual: solo owner/admin (canDelete = nivel admin+)
                        // Muestra badge si hay ops pendientes en la cola local.
                        if (ui.canDelete) {
                            BadgedBox(
                                badge = {
                                    if (ui.pendingOpsCount > 0) {
                                        Badge { Text(ui.pendingOpsCount.toString()) }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = vm::forceSync,
                                    enabled = !ui.isForceSyncing,
                                ) {
                                    if (ui.isForceSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = "Forzar sincronización")
                                    }
                                }
                            }
                        }
                        // Reasignación masiva: solo manager+ (canCreate cubre owner/admin/manager)
                        if (ui.canCreate && ui.routes.isNotEmpty()) {
                            IconButton(onClick = vm::enterSelectionMode) {
                                Icon(Icons.Default.Checklist,
                                    contentDescription = "Seleccionar rutas")
                            }
                        }
                        // Importar: owner, admin y manager pueden importar rutas
                        if (ui.canCreate) {
                            IconButton(onClick = onImport) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Importar CSV")
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (ui.canCreate && !ui.selectionMode) {
                FloatingActionButton(onClick = vm::onShowCreateDialog) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva ruta")
                }
            }
        },
        bottomBar = {
            if (ui.selectionMode) {
                BottomAppBar {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${ui.selectedRouteUids.size} ruta(s)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = vm::onShowBulkDialog,
                        enabled = ui.selectedRouteUids.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reasignar")
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = ui.isSyncing,
            onRefresh    = vm::syncNow,
            state        = pullState,
            modifier     = Modifier.fillMaxSize().padding(padding),
        ) {
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            ui.routes.isEmpty() -> Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Sin rutas", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Pulsa + para crear la primera",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = vm::syncNow,
                        enabled = !ui.isSyncing,
                    ) {
                        if (ui.isSyncing) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp), strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Sincronizando…")
                        } else {
                            Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sincronizar ahora")
                        }
                    }
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(ui.routes, key = { _, r -> r.uid }) { idx, route ->
                    val agentName = if (route.userId != ui.currentUserId)
                        ui.teamMembers[route.userId] else null
                    RouteListItem(
                        route         = route,
                        stopCount     = ui.stopCounts[route.uid],
                        onClick       = {
                            if (ui.selectionMode) vm.toggleRouteSelection(route.uid)
                            else onRouteClick(route.uid)
                        },
                        testTagId     = "route-card-$idx",
                        agentName     = agentName,
                        selectionMode = ui.selectionMode,
                        selected      = route.uid in ui.selectedRouteUids,
                        onLongClick   = {
                            if (!ui.selectionMode && ui.canCreate) {
                                vm.enterSelectionMode()
                                vm.toggleRouteSelection(route.uid)
                            }
                        },
                    )
                }
            }
        }
        } // PullToRefreshBox
    }

    // ── Diálogo crear ruta ────────────────────────────────────
    if (ui.showCreateDialog) {
        AlertDialog(
            onDismissRequest = vm::onDismissCreateDialog,
            title = { Text("Nueva ruta") },
            text = {
                var expanded by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value         = ui.newRouteName,
                        onValueChange = vm::onNewRouteNameChange,
                        label         = { Text("Nombre de la ruta") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        isError       = ui.error != null,
                        supportingText = ui.error?.let { err ->
                            { Text(err, color = MaterialTheme.colorScheme.error) }
                        },
                    )

                    // Selector de asignado — solo visible si hay usuarios disponibles
                    if (ui.assignableUsers.isNotEmpty()) {
                        val selectedName = ui.assignableUsers
                            .firstOrNull { it.userId == ui.selectedAssigneeId }
                            ?.let { "${it.displayName} (${it.role})" }
                            ?: "Para mí mismo"
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
                                DropdownMenuItem(
                                    text    = { Text("Para mí mismo") },
                                    onClick = { vm.onSelectAssignee(null); expanded = false },
                                )
                                ui.assignableUsers.forEach { user ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(user.displayName,
                                                    style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    "${user.role} · @${user.username}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        onClick = { vm.onSelectAssignee(user.userId); expanded = false },
                                    )
                                }
                            }
                        }
                    } else if (ui.loadingUsers) {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Cargando equipo…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = vm::createRoute) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = vm::onDismissCreateDialog) { Text("Cancelar") }
            },
        )
    }

    // ── Diálogo reasignación masiva ───────────────────────────
    if (ui.showBulkDialog) {
        AlertDialog(
            onDismissRequest = { if (!ui.isBulkAssigning) vm.onDismissBulkDialog() },
            title = { Text("Reasignar ${ui.selectedRouteUids.size} ruta(s)") },
            text = {
                var expanded by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (ui.assignableUsers.isEmpty() && ui.loadingUsers) {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Cargando equipo…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val selectedName = ui.assignableUsers
                            .firstOrNull { it.userId == ui.bulkAssigneeId }
                            ?.let { "${it.displayName} (${it.role})" }
                            ?: "Selecciona destinatario"
                        ExposedDropdownMenuBox(
                            expanded         = expanded,
                            onExpandedChange = { expanded = !expanded },
                        ) {
                            OutlinedTextField(
                                value         = selectedName,
                                onValueChange = {},
                                readOnly      = true,
                                label         = { Text("Reasignar a") },
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
                                                Text(
                                                    "${user.role} · @${user.username}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        onClick = { vm.onBulkAssigneeChange(user.userId); expanded = false },
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value         = ui.bulkReason,
                        onValueChange = vm::onBulkReasonChange,
                        label         = { Text("Motivo (opcional)") },
                        singleLine    = false,
                        minLines      = 2,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = vm::reassignSelectedRoutes,
                    enabled = !ui.isBulkAssigning && ui.bulkAssigneeId != null,
                ) {
                    if (ui.isBulkAssigning) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Reasignar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = vm::onDismissBulkDialog,
                    enabled = !ui.isBulkAssigning,
                ) { Text("Cancelar") }
            },
        )
    }
}


@Composable
private fun StatusChip(status: String) {
    val st = RouteStatusTokens.of(status)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(st.container, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(st.icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = st.color)
        Text(st.label, style = MaterialTheme.typography.labelMedium, color = st.color)
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RouteListItem(
    route: RouteEntity,
    onClick: () -> Unit,
    stopCount: com.pabl3st.rutapp.data.local.dao.RouteStopCount? = null,
    testTagId: String = "",
    agentName: String? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: () -> Unit = {},
) {
    val statusTokens = RouteStatusTokens.of(route.status)

    val cardColors = if (selected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else CardDefaults.cardColors()

    Card(
        colors = cardColors,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = testTagId }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Acento lateral — color del estado de la ruta
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusTokens.color),
            )
            Row(
                modifier          = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    Checkbox(checked = selected, onCheckedChange = { onClick() })
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    // Fila 1 — nombre + chip de estado
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            route.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusChip(status = route.status)
                    }

                    Spacer(Modifier.height(6.dp))

                    // Fila 2 — fecha con distancia relativa
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.CalendarToday, null, Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            routeDateLabel(route.dateAssigned),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Fila 3 — progreso de paradas (solo si la ruta tiene paradas en local)
                    if (stopCount != null && stopCount.total > 0) {
                        Spacer(Modifier.height(8.dp))
                        val fraction = (stopCount.done.toFloat() / stopCount.total)
                            .coerceIn(0f, 1f)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color     = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                                drawStopIndicator = {},
                            )
                            Text(
                                "${stopCount.done} / ${stopCount.total} paradas",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Fila 4 — agente + estado de sync
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        agentName?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(Icons.Default.Person, null, Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Text(it, style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (route.syncStatus != "synced") {
                            val syncError = route.syncStatus == "error"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    if (syncError) Icons.Default.SyncProblem else Icons.Default.CloudOff,
                                    null, Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    if (syncError) "Error de sync" else "Sin sincronizar",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    route.notes?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** Etiqueta de fecha de ruta: "Hoy", "Mañana · en 1 día", "3 jun 2026 · en 7 días". */
private fun routeDateLabel(dateAssigned: String): String {
    if (dateAssigned == "1970-01-01" || dateAssigned.isBlank()) return "Sin fecha"
    return runCatching {
        val d     = LocalDate.parse(dateAssigned)
        val today = LocalDate.now()
        val days  = java.time.temporal.ChronoUnit.DAYS.between(today, d)
        val pretty = d.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es")))
        when {
            days == 0L  -> "Hoy"
            days == 1L  -> "Mañana · en 1 día"
            days == -1L -> "Ayer"
            days > 1L   -> "$pretty · en $days días"
            else        -> "$pretty · hace ${-days} días"
        }
    }.getOrDefault(dateAssigned)
}

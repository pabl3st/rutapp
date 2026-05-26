@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.rutas

import com.pabl3st.rutapp.core.ui.theme.RouteStatusTokens

import androidx.compose.foundation.clickable
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

    Scaffold(
        topBar = {
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
                    // Importar: owner, admin y manager pueden importar rutas
                    if (ui.canCreate) {
                        IconButton(onClick = onImport) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Importar CSV")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (ui.canCreate) {
                FloatingActionButton(onClick = vm::onShowCreateDialog) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva ruta")
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
                Modifier.fillMaxSize().padding(padding),
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
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding),
            ) {
                itemsIndexed(ui.routes, key = { _, r -> r.uid }) { idx, route ->
                    val agentName = if (route.userId != ui.currentUserId)
                        ui.teamMembers[route.userId] else null
                    RouteListItem(
                        route     = route,
                        onClick   = { onRouteClick(route.uid) },
                        testTagId = "route-card-$idx",
                        agentName = agentName,
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
}


@Composable
private fun StatusChip(status: String) {
    val st = RouteStatusTokens.of(status)
    val color = st.color; val icon = st.icon; val label = st.label
    SuggestionChip(
        onClick = {},
        label   = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon    = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors  = SuggestionChipDefaults.suggestionChipColors(
            labelColor         = color,
            iconContentColor   = color,
        ),
    )
}
@Composable
private fun RouteListItem(route: RouteEntity, onClick: () -> Unit, testTagId: String = "", agentName: String? = null) {

    Card(modifier = Modifier.fillMaxWidth().semantics { testTag = testTagId }.clickable(onClick = onClick)) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(route.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                val today = LocalDate.now().toString()
                val dateLabel = if (route.dateAssigned == "1970-01-01" || route.dateAssigned.isBlank()) {
                    "Sin fecha"
                } else {
                    runCatching {
                        val d = LocalDate.parse(route.dateAssigned)
                        when (route.dateAssigned) {
                            today -> "Hoy"
                            LocalDate.now().minusDays(1).toString() -> "Ayer"
                            LocalDate.now().plusDays(1).toString()  -> "Mañana"
                            else  -> d.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es")))
                        }
                    }.getOrDefault(route.dateAssigned)
                }
                Text(dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                agentName?.let {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Person, null, Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Text(it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                route.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(status = route.status)
                if (route.syncStatus == "pending") {
                    Icon(Icons.Default.CloudOff, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }
}

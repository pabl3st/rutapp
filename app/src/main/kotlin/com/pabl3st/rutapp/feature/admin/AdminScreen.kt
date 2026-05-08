@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.network.AccountUserDto

@Composable
fun AdminScreen(
    onBack: () -> Unit = {},
    vm: AdminViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(ui.error) {
        ui.error?.let { snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short); vm.clearError() }
    }
    LaunchedEffect(ui.snackbar) {
        ui.snackbar?.let { snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short); vm.clearSnackbar() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Admin") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                },
                actions = {
                    if (ui.canManageUsers) {
                        IconButton(onClick = vm::loadUsers) {
                            Icon(Icons.Default.Refresh, "Recargar usuarios")
                        }
                        IconButton(onClick = vm::onShowInviteDialog) {
                            Icon(Icons.Default.PersonAdd, "Invitar usuario")
                        }
                    }
                },
            )
        }
    ) { padding ->
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {

            // ── Sesión activa ─────────────────────────────────
            item {
                SectionTitle("Sesión activa")
                Spacer(Modifier.height(Spacing.sm))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        AdminInfoRow(Icons.Default.Person,   "Nombre", ui.userName)
                        AdminInfoRow(Icons.Default.Email,    "Email",  ui.userEmail)
                        AdminInfoRow(Icons.Default.Badge,    "Rol",    vm.roleLabel(ui.userRole))
                        AdminInfoRow(Icons.Default.Business, "Cuenta", ui.accountName)
                    }
                }
            }

            // ── Estadísticas globales ─────────────────────────
            item {
                SectionTitle("Estadísticas")
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    AdminStatCard(Modifier.weight(1f), Icons.Default.Route, "${ui.totalRoutes}", "Rutas")
                    AdminStatCard(Modifier.weight(1f), Icons.Default.Place, "${ui.totalStops}",  "Stops")
                }
                Spacer(Modifier.height(Spacing.sm))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = if (ui.pendingSync > 0)
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                    else CardDefaults.cardColors(),
                ) {
                    Row(
                        modifier          = Modifier.padding(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (ui.pendingSync > 0) Icons.Default.CloudOff else Icons.Default.CloudDone,
                            null,
                            Modifier.size(20.dp),
                            tint = if (ui.pendingSync > 0) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(Spacing.md))
                        Text(
                            if (ui.pendingSync > 0) "${ui.pendingSync} operaciones pendientes de sync"
                            else "Sincronización al día",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // ── Usuarios del account ──────────────────────────
            if (ui.canManageUsers) {
                item {
                    SectionTitle("Usuarios del equipo")
                    Spacer(Modifier.height(Spacing.sm))
                }

                if (ui.usersLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                } else if (ui.users.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.padding(Spacing.lg)) {
                                Text(
                                    "No hay usuarios en el equipo todavía. Invita a alguien.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(ui.users, key = { it.userId }) { user ->
                        UserCard(
                            user           = user,
                            currentUserId  = 0, // No revelamos ID propio, gestionamos con session
                            roleLabel      = vm::roleLabel,
                            onChangeRole   = { vm.onShowRolePicker(user) },
                            onDeactivate   = { vm.deactivateUser(user) },
                            canEdit        = ui.userRole in setOf("owner", "admin") && user.role != "owner",
                        )
                    }
                }
            }

            // ── Info del build ────────────────────────────────
            item {
                SectionTitle("Aplicación")
                Spacer(Modifier.height(Spacing.sm))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        AdminInfoRow(Icons.Default.Android, "Versión", "1.0.0-s14")
                        AdminInfoRow(Icons.Default.Storage, "BD Local", "Room v6")
                        AdminInfoRow(Icons.Default.Cloud,   "API",      "v1.1.0")
                        AdminInfoRow(Icons.Default.Map,     "Mapa",     "MapLibre 11.5.1")
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.xl)) }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────
    if (ui.showInviteDialog) {
        InviteDialog(ui = ui, vm = vm)
    }
    if (ui.showRolePicker) {
        RolePickerDialog(ui = ui, vm = vm)
    }
}

// ─────────────────────────────────────────────────────────────

@Composable
private fun UserCard(
    user:          AccountUserDto,
    currentUserId: Int,
    roleLabel:     (String) -> String,
    onChangeRole:  () -> Unit,
    onDeactivate:  () -> Unit,
    canEdit:       Boolean,
) {
    var showConfirmDeactivate by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (!user.isActive)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar placeholder
            Surface(
                modifier = Modifier.size(40.dp),
                shape    = MaterialTheme.shapes.small,
                color    = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(user.displayName, style = MaterialTheme.typography.bodyMedium)
                    if (!user.isActive) {
                        SuggestionChip(
                            onClick  = {},
                            label    = { Text("Inactivo", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(20.dp),
                        )
                    }
                }
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AssistChip(
                    onClick    = { if (canEdit) onChangeRole() },
                    label      = { Text(roleLabel(user.role), style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = if (canEdit) { { Icon(Icons.Default.Edit, null, Modifier.size(14.dp)) } } else null,
                    modifier   = Modifier.height(24.dp),
                )
            }
            if (canEdit && user.isActive) {
                IconButton(onClick = { showConfirmDeactivate = true }) {
                    Icon(Icons.Default.PersonOff, "Desactivar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showConfirmDeactivate) {
        AlertDialog(
            onDismissRequest = { showConfirmDeactivate = false },
            title  = { Text("Desactivar usuario") },
            text   = { Text("¿Desactivar a ${user.displayName}? Perderá acceso a la app.") },
            confirmButton = {
                Button(
                    onClick = { showConfirmDeactivate = false; onDeactivate() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Desactivar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeactivate = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun InviteDialog(ui: AdminUiState, vm: AdminViewModel) {
    AlertDialog(
        onDismissRequest = vm::onDismissInviteDialog,
        title = { Text("Invitar usuario") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = ui.inviteEmail,
                    onValueChange = vm::onInviteEmailChange,
                    label         = { Text("Email *") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier      = Modifier.fillMaxWidth(),
                )
                Text("Rol", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    vm.availableRoles.forEach { role ->
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = ui.inviteRole == role,
                                onClick  = { vm.onInviteRoleChange(role) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(vm.roleLabel(role), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = vm::sendInvite) { Text("Enviar invitación") } },
        dismissButton = { TextButton(onClick = vm::onDismissInviteDialog) { Text("Cancelar") } },
    )
}

@Composable
private fun RolePickerDialog(ui: AdminUiState, vm: AdminViewModel) {
    val user = ui.rolePickerUser ?: return
    AlertDialog(
        onDismissRequest = vm::onDismissRolePicker,
        title = { Text("Cambiar rol de ${user.displayName}") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                vm.availableRoles.forEach { role ->
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = user.role == role,
                            onClick  = { vm.onSelectRole(role) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(vm.roleLabel(role), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton  = {},
        dismissButton  = { TextButton(onClick = vm::onDismissRolePicker) { Text("Cancelar") } },
    )
}

// ── Helpers ───────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun AdminInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AdminStatCard(modifier: Modifier = Modifier, icon: ImageVector, value: String, label: String) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(Spacing.xs))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

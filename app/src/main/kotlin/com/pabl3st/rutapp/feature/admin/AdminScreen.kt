@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
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
import com.pabl3st.rutapp.BuildConfig
import com.pabl3st.rutapp.data.network.AccountUserDto
import com.pabl3st.rutapp.data.network.InviteDto
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun AdminScreen(
    onBack:            () -> Unit         = {},
    onNavigateToTeam:  (() -> Unit)?      = null,
    onNavigateToAgent: ((Int) -> Unit)?   = null,
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
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
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

            // ── Mi equipo (manager/admin/owner/god) ───────────────
            if (ui.showDirectReports) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically,
                    ) {
                        SectionTitle("Mi equipo · hoy")
                        if (onNavigateToTeam != null) {
                            Button(
                                onClick  = onNavigateToTeam,
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                            ) {
                                Icon(Icons.Default.Group, null, Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ver todo", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }
                if (ui.directReportsLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                } else if (ui.directReports.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Box(Modifier.padding(Spacing.lg)) {
                                Text(
                                    "No tienes reportadores directos asignados todavía.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(ui.directReports, key = { "dr_${it.userId}" }) { reporter ->
                        val kpiStats = ui.reporterServerStats.firstOrNull { it.userId == reporter.userId }
                        DirectReporterCard(
                            reporter     = reporter,
                            routeCount   = ui.reporterRouteCounts[reporter.userId]  ?: 0,
                            doneStops    = ui.reporterDoneStops[reporter.userId]    ?: 0,
                            pendingStops = ui.reporterPendingStops[reporter.userId] ?: 0,
                            contacted    = kpiStats?.contacted  ?: 0,
                            totalStops   = kpiStats?.totalStops ?: 0,
                            onClick      = onNavigateToAgent?.let { nav -> { nav(reporter.userId) } },
                        )
                    }
                }
                item { Spacer(Modifier.height(Spacing.sm)) }
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
                            user            = user,
                            roleLabel       = vm::roleLabel,
                            onChangeRole    = { vm.onShowRolePicker(user) },
                            onDeactivate    = { vm.deactivateUser(user) },
                            onReactivate    = { vm.reactivateUser(user) },
                            onAssignManager = { vm.onShowManagerPicker(user) },
                            canEdit         = ui.userRole == "god" || (ui.userRole in setOf("owner", "admin") && user.role !in setOf("owner", "god")),
                        )
                    }
                }
            }

            // ── Invitaciones activas ─────────────────────────
            if (ui.canManageUsers && (ui.invites.isNotEmpty() || ui.invitesLoading)) {
                item {
                    SectionTitle("Invitaciones activas")
                    Spacer(Modifier.height(Spacing.sm))
                }
                if (ui.invitesLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(Spacing.md), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                } else {
                    items(ui.invites, key = { it.id }) { invite ->
                        InviteCard(
                            invite   = invite,
                            roleLabel = vm::roleLabel,
                            onDelete  = { vm.deleteInvite(invite.id) },
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
                        AdminInfoRow(Icons.Default.Android, "Versión", BuildConfig.VERSION_NAME)
                        AdminInfoRow(Icons.Default.Storage, "BD Local", "Room v12")
                        AdminInfoRow(Icons.Default.Cloud,   "API",      "v1.1.0 (api.php)")
                        AdminInfoRow(Icons.Default.Map,     "Mapa",     "MapLibre 11.5.1")
                    }
                }
            }

            // ── Zona de peligro — solo owner/god ────────────
            if (ui.userRole in listOf("owner", "god")) {
                item {
                    Spacer(Modifier.height(Spacing.md))
                    HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    Spacer(Modifier.height(Spacing.sm))
                    Text("Zona de peligro",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 2.dp))
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(
                        onClick  = vm::onClearRoutesRequest,
                        modifier = Modifier.fillMaxWidth(),
                        enabled  = !ui.isClearingRoutes,
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error),
                        border   = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    ) {
                        if (ui.isClearingRoutes) {
                            CircularProgressIndicator(Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.error, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.DeleteForever, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Borrar todas las rutas y paradas")
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.xl)) }
        }
    }

    // ── Dialog borrar rutas ───────────────────────────────────
    if (ui.showClearDialog) {
        AlertDialog(
            onDismissRequest = vm::onClearRoutesDismiss,
            icon  = { Icon(Icons.Default.Warning, null,
                tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Borrar todo el contenido?") },
            text  = {
                Text("Se eliminarán TODAS las rutas, paradas e historial de visitas " +
                     "de la cuenta. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(onClick = vm::confirmClearRoutes,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Sí, borrar todo") }
            },
            dismissButton = {
                TextButton(onClick = vm::onClearRoutesDismiss) { Text("Cancelar") }
            },
        )
    }

    // ── Dialogs ───────────────────────────────────────────────
    if (ui.showInviteDialog) {
        InviteDialog(ui = ui, vm = vm)
    }
    if (ui.showRolePicker) {
        RolePickerDialog(ui = ui, vm = vm)
    }
    ui.generatedCode?.let { code ->
        InviteCodeDialog(code = code, roleLabel = vm.roleLabel(ui.inviteRole), onDismiss = vm::onDismissGeneratedCode)
    }
    if (ui.showManagerPicker) {
        ui.managerPickerUser?.let { target ->
            ManagerPickerDialog(
                user       = target,
                candidates = vm.validManagersFor(target.role),
                roleLabel  = vm::roleLabel,
                onSelect   = vm::onAssignManager,
                onDismiss  = vm::onDismissManagerPicker,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────

@Composable
private fun UserCard(
    user:          AccountUserDto,
    roleLabel:     (String) -> String,
    onChangeRole:  () -> Unit,
    onDeactivate:  () -> Unit,
    onReactivate:    () -> Unit,
    onAssignManager: () -> Unit,
    canEdit:         Boolean,
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
                user.managerName?.let { mName ->
                    Text(
                        text  = "↳ $mName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    )
                }
                AssistChip(
                    onClick    = { if (canEdit) onChangeRole() },
                    label      = { Text(roleLabel(user.role), style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = if (canEdit) {
                        val icon: @Composable () -> Unit = { Icon(Icons.Default.Edit, null, Modifier.size(14.dp)) }
                        icon
                    } else null,
                    modifier   = Modifier.height(24.dp),
                )
            }
            if (canEdit) {
                IconButton(onClick = onAssignManager) {
                    Icon(Icons.Default.SupervisedUserCircle, "Asignar supervisor",
                        modifier = Modifier.size(18.dp),
                        tint     = if (user.managerId != null)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (user.isActive) {
                    IconButton(onClick = { showConfirmDeactivate = true }) {
                        Icon(Icons.Default.PersonOff, "Desactivar",
                            tint     = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                    }
                } else {
                    IconButton(onClick = onReactivate) {
                        Icon(Icons.Default.PersonAdd, "Reactivar",
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                    }
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
        confirmButton = {
            Button(
                onClick  = vm::sendInvite,
                enabled  = !ui.isSendingInvite,
            ) {
                if (ui.isSendingInvite) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Generar código")
            }
        },
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
                    val desc = when (role) {
                        "admin"   -> "Administra usuarios y rutas"
                        "manager" -> "Ve rutas de todo el account"
                        "agent"   -> "Ejecuta sus propias rutas"
                        "viewer"  -> "Solo lectura"
                        else      -> ""
                    }
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = user.role == role,
                            onClick  = { vm.onSelectRole(role) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(vm.roleLabel(role), style = MaterialTheme.typography.bodyMedium)
                            if (desc.isNotEmpty()) Text(desc, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton  = {},
        dismissButton  = { TextButton(onClick = vm::onDismissRolePicker) { Text("Cancelar") } },
    )
}

@Composable
private fun InviteCard(
    invite:    InviteDto,
    roleLabel: (String) -> String,
    onDelete:  () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        invite.code,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(
                        onClick  = { clipboard.setText(AnnotatedString(invite.code)) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copiar código", Modifier.size(14.dp))
                    }
                }
                Text(
                    "Rol: ${roleLabel(invite.roleToAssign)} · ${invite.usesLeft} uso${if (invite.usesLeft != 1) "s" else ""} restante${if (invite.usesLeft != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Expira: ${invite.expiresAt.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, "Eliminar invitación",
                    Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun InviteCodeDialog(code: String, roleLabel: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.Key, null) },
        title = { Text("Código de invitación") },
        text  = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    "Comparte este código con la persona que quieres invitar como $roleLabel:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Surface(
                    color  = MaterialTheme.colorScheme.primaryContainer,
                    shape  = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(Spacing.lg),
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            code,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(Spacing.md))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                            Icon(Icons.Default.ContentCopy, "Copiar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                Text(
                    "El código es válido por 7 días y para 1 uso.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton  = { Button(onClick = onDismiss) { Text("Entendido") } },
        dismissButton  = null,
    )
}

@Composable
private fun ManagerPickerDialog(
    user:       AccountUserDto,
    candidates: List<AccountUserDto>,
    roleLabel:  (String) -> String,
    onSelect:   (Int?) -> Unit,
    onDismiss:  () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.SupervisedUserCircle, null) },
        title = { Text("Supervisor de ${user.displayName}") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    "El supervisor podrá ver las rutas y KPIs de este usuario.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = user.managerId == null, onClick = { onSelect(null) })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Sin supervisor", style = MaterialTheme.typography.bodyMedium)
                        Text("No reporta a nadie", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (candidates.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = Spacing.xs))
                    candidates.forEach { candidate ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = user.managerId == candidate.userId,
                                onClick  = { onSelect(candidate.userId) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(candidate.displayName, style = MaterialTheme.typography.bodyMedium)
                                Text("@${candidate.username} · ${roleLabel(candidate.role)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Text("No hay usuarios con rol superior disponibles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton  = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        dismissButton  = null,
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


// ── Panel de reportador directo ────────────────────────────
@Composable
private fun DirectReporterCard(
    reporter:     AccountUserDto,
    routeCount:   Int,
    doneStops:    Int,
    pendingStops: Int,
    contacted:    Int = 0,
    totalStops:   Int = 0,
    onClick:      (() -> Unit)? = null,
) {
    Card(
        onClick   = onClick ?: {},
        modifier  = Modifier.fillMaxWidth(),
        enabled   = onClick != null,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // Cabecera: nombre + rol
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector        = Icons.Default.Person,
                    contentDescription = null,
                    modifier           = Modifier.size(20.dp),
                    tint               = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(reporter.displayName, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        "@${reporter.username} · ${reporter.role}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Rutas del día
                if (routeCount > 0) {
                    Surface(
                        shape  = MaterialTheme.shapes.small,
                        color  = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            "$routeCount ${if (routeCount == 1) "ruta" else "rutas"}",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            // Stats de paradas si tiene rutas hoy
            if (doneStops + pendingStops > 0) {
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    // Progreso visual
                    val total    = doneStops + pendingStops
                    val progress = if (total > 0) doneStops.toFloat() / total else 0f
                    LinearProgressIndicator(
                        progress          = { progress },
                        modifier          = Modifier.weight(1f).align(Alignment.CenterVertically),
                        trackColor        = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        "$doneStops/$total paradas",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (doneStops == total)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Sin rutas asignadas hoy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // KPIs del mes desde servidor (si disponibles)
            if (totalStops > 0 || contacted > 0) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalStops", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("Paradas mes", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$contacted", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Contactadas", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (totalStops > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val pct = (contacted * 100f / totalStops).toInt()
                            Text("$pct%", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (pct >= 80) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Eficiencia", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

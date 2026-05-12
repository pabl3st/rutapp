@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.BuildConfig
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.network.GodAccountDto
import com.pabl3st.rutapp.data.network.GodUserDto

@Composable
fun GodDashboardScreen(
    onBack: () -> Unit = {},
    vm: GodDashboardViewModel = hiltViewModel(),
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
                title = {
                    Column {
                        Text("God Dashboard", fontWeight = FontWeight.Bold)
                        Text("Superadmin · ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                actions = {
                    IconButton(onClick = vm::loadStats) {
                        Icon(Icons.Default.Refresh, "Recargar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { padding ->

        Column(modifier = Modifier.padding(padding)) {

            // ── Tab bar ───────────────────────────────────────
            TabRow(selectedTabIndex = GodTab.entries.indexOf(ui.activeTab)) {
                GodTab.entries.forEach { tab ->
                    Tab(
                        selected = ui.activeTab == tab,
                        onClick  = { vm.onTabChange(tab) },
                        text     = { Text(tab.label) },
                    )
                }
            }

            when (ui.activeTab) {
                GodTab.OVERVIEW  -> OverviewTab(ui = ui)
                GodTab.ACCOUNTS  -> AccountsTab(ui = ui)
                GodTab.USERS     -> UsersTab(ui = ui, vm = vm)
            }
        }
    }

    // ── Role picker dialog ────────────────────────────────────
    if (ui.showRolePicker) {
        RolePickerGodDialog(ui = ui, vm = vm)
    }
}

// ══════════════════════════════════════════════════════════════
// TAB 1 — RESUMEN GLOBAL
// ══════════════════════════════════════════════════════════════
@Composable
private fun OverviewTab(ui: GodDashboardUiState) {
    if (ui.isLoadingStats) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Stats globales
        item {
            GodSectionTitle("Sistema global")
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GodStatCard(Modifier.weight(1f), Icons.Default.Business,    "${ui.totalAccounts}", "Cuentas")
                GodStatCard(Modifier.weight(1f), Icons.Default.Group,       "${ui.totalUsers}",    "Usuarios")
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GodStatCard(Modifier.weight(1f), Icons.Default.Route,  "${ui.totalRoutes}",  "Rutas")
                GodStatCard(Modifier.weight(1f), Icons.Default.Place,  "${ui.totalStops}",   "Stops")
                GodStatCard(Modifier.weight(1f), Icons.Default.BarChart, "${ui.totalReports}", "KPIs")
            }
        }

        // Usuarios recientes
        if (ui.recentUsers.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Spacing.sm))
                GodSectionTitle("Usuarios recientes (7 días)")
            }
            items(ui.recentUsers) { user ->
                GodUserRowCompact(user)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// TAB 2 — CUENTAS
// ══════════════════════════════════════════════════════════════
@Composable
private fun AccountsTab(ui: GodDashboardUiState) {
    if (ui.isLoadingStats) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (ui.topAccounts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin datos de cuentas", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            GodSectionTitle("Top cuentas por actividad")
            Spacer(Modifier.height(Spacing.sm))
        }
        items(ui.topAccounts) { account ->
            GodAccountCard(account)
        }
    }
}

@Composable
private fun GodAccountCard(account: GodAccountDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Business, null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("ID ${account.id} · ${account.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                account.lastActivity?.let {
                    Text("Última actividad: ${it.take(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${account.userCount} usuarios",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text("${account.routeCount} rutas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// TAB 3 — USUARIOS
// ══════════════════════════════════════════════════════════════
@Composable
private fun UsersTab(ui: GodDashboardUiState, vm: GodDashboardViewModel) {
    Column {
        // Barra de búsqueda + filtro de rol
        Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
            OutlinedTextField(
                value         = ui.userSearch,
                onValueChange = vm::onUserSearchChange,
                placeholder   = { Text("Buscar por nombre, email…") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = {
                    if (ui.userSearch.isNotEmpty())
                        IconButton(onClick = { vm.onUserSearchChange(""); vm.applyUserSearch() }) {
                            Icon(Icons.Default.Clear, null)
                        }
                },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.applyUserSearch() }),
                modifier      = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.xs))
            // Filtro de rol — chips horizontales
            var roleExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = it },
            ) {
                OutlinedTextField(
                    value = if (ui.roleFilter.isBlank()) "Todos los roles" else vm.roleLabel(ui.roleFilter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtrar por rol") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                    modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                    vm.roleOptions.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(if (role.isBlank()) "Todos" else vm.roleLabel(role)) },
                            onClick = { vm.onRoleFilterChange(role); roleExpanded = false },
                        )
                    }
                }
            }
        }

        if (ui.isLoadingUsers) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (ui.allUsers.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) {
                        Text("Sin usuarios", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(ui.allUsers, key = { it.id }) { user ->
                    GodUserCard(
                        user      = user,
                        roleLabel = vm::roleLabel,
                        canEdit   = vm.canEditUser(user.role),
                        onChangeRole = { vm.onShowRolePicker(user) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GodUserCard(
    user:        GodUserDto,
    roleLabel:   (String) -> String,
    canEdit:     Boolean,
    onChangeRole: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (!user.isActive)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(40.dp),
                shape    = MaterialTheme.shapes.small,
                color    = when (user.role) {
                    "god"   -> MaterialTheme.colorScheme.error
                    "owner" -> MaterialTheme.colorScheme.primary
                    "admin" -> MaterialTheme.colorScheme.secondary
                    else    -> MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = when (user.role) {
                            "god"   -> MaterialTheme.colorScheme.onError
                            "owner" -> MaterialTheme.colorScheme.onPrimary
                            "admin" -> MaterialTheme.colorScheme.onSecondary
                            else    -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(user.displayName, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                    if (!user.isActive) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Inactivo", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(20.dp),
                        )
                    }
                }
                Text(user.email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(user.accountName, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                AssistChip(
                    onClick = { if (canEdit) onChangeRole() },
                    label   = { Text(roleLabel(user.role), style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = if (canEdit) {
                        { Icon(Icons.Default.Edit, null, Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.height(24.dp),
                )
            }
            Text(
                user.createdAt.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GodUserRowCompact(user: GodUserDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape    = MaterialTheme.shapes.small,
                color    = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(user.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(user.accountName, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SuggestionChip(
                onClick = {},
                label = { Text(user.role, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(20.dp),
            )
            Text(user.createdAt.take(10), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Role picker dialog ────────────────────────────────────────
@Composable
private fun RolePickerGodDialog(ui: GodDashboardUiState, vm: GodDashboardViewModel) {
    val user = ui.rolePickerUser ?: return
    AlertDialog(
        onDismissRequest = vm::onDismissRolePicker,
        icon  = { Icon(Icons.Default.AdminPanelSettings, null) },
        title = { Text("Cambiar rol de ${user.displayName}") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cuenta: ${user.accountName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.sm))
                listOf("god","owner","admin","manager","agent","viewer").forEach { role ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = ui.pendingNewRole == role,
                            onClick  = { vm.onPendingRoleChange(role) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(vm.roleLabel(role), style = MaterialTheme.typography.bodyMedium)
                            val desc = when (role) {
                                "god"     -> "Acceso total al sistema"
                                "owner"   -> "Gestiona su account completo"
                                "admin"   -> "Administra usuarios y rutas"
                                "manager" -> "Ve rutas de todo el account"
                                "agent"   -> "Ejecuta sus propias rutas"
                                "viewer"  -> "Solo lectura"
                                else -> ""
                            }
                            if (desc.isNotBlank())
                                Text(desc, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = vm::confirmRoleChange,
                enabled = ui.pendingNewRole != user.role,
            ) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = vm::onDismissRolePicker) { Text("Cancelar") }
        },
    )
}

// ── Helpers ───────────────────────────────────────────────────
@Composable
private fun GodSectionTitle(text: String) {
    Text(
        text,
        style      = MaterialTheme.typography.titleSmall,
        color      = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun GodStatCard(
    modifier: Modifier = Modifier,
    icon:     ImageVector,
    value:    String,
    label:    String,
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
    ) {
        Column(
            modifier              = Modifier.padding(Spacing.md),
            horizontalAlignment   = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

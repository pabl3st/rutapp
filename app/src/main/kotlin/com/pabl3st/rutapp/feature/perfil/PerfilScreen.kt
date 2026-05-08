@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.ThemeMode
import com.pabl3st.rutapp.core.ui.theme.ThemeViewModel
import com.pabl3st.rutapp.core.ui.theme.Spacing

@Composable
fun PerfilScreen(
    onLoggedOut:                 () -> Unit,
    onBack:                      () -> Unit,
    onNavigateToBusinessProfile: () -> Unit = {},
    onNavigateToCalendario:      () -> Unit = {},
    onNavigateToAdmin:           () -> Unit = {},
    vm:      PerfilViewModel  = hiltViewModel(),
    themeVm: ThemeViewModel   = hiltViewModel(),
) {
    val ui        by vm.ui.collectAsStateWithLifecycle()
    val prefs     by vm.userPrefs.collectAsStateWithLifecycle()
    val themeMode by themeVm.themeMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {

            // ── Avatar ────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier          = Modifier.padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape    = MaterialTheme.shapes.extraLarge,
                        color    = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                ui.displayName.firstOrNull()?.uppercase() ?: "?",
                                style      = MaterialTheme.typography.titleLarge,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.width(Spacing.lg))
                    Column {
                        Text(ui.displayName,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("@${ui.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(Spacing.xs))
                        RolChip(ui.role)
                    }
                }
            }

            // ── Cuenta ────────────────────────────────────────
            SectionTitle("Cuenta")
            InfoCard {
                InfoRow(Icons.Default.Email,            "Email",   ui.email)
                Div()
                InfoRow(Icons.Default.Business,         "Empresa", ui.accountName)
                Div()
                InfoRow(Icons.Default.WorkspacePremium, "Plan",
                    ui.plan.replaceFirstChar { it.uppercase() })
            }

            // ── Ajustes generales ─────────────────────────────
            SectionTitle("Ajustes generales")
            InfoCard {
                // Apariencia
                ThemeRow(currentMode = themeMode, onSelect = themeVm::setTheme)
                Div()
                // Idioma
                LanguageRow(current = prefs.language, onSelect = vm::setLanguage)
            }

            // ── Formulario de visita ──────────────────────────
            SectionTitle("Formulario de visita")
            InfoCard {
                // Tipo de negocio / sector → BusinessProfileScreen
                InfoRowNav(
                    icon   = Icons.Default.BusinessCenter,
                    label  = "Tipo de negocio",
                    detail = ui.sectorLabel,
                    onClick = onNavigateToBusinessProfile,
                )
                Div()
                // KPIs activos → BusinessProfileScreen
                InfoRowNav(
                    icon   = Icons.Default.BarChart,
                    label  = "KPIs activos",
                    detail = "Gestionar campos del sector",
                    onClick = onNavigateToBusinessProfile,
                )
                Div()
                SwitchRow(
                    icon    = Icons.Default.Timer,
                    label   = "Mostrar duración",
                    detail  = "Campo de minutos en el formulario",
                    checked = prefs.showVisitDuration,
                    onToggle = vm::setShowVisitDuration,
                )
                Div()
                SwitchRow(
                    icon    = Icons.Default.NextPlan,
                    label   = "Mostrar próxima acción",
                    detail  = "Campo de planificación de siguiente visita",
                    checked = prefs.showNextAction,
                    onToggle = vm::setShowNextAction,
                )
                Div()
                SwitchRow(
                    icon    = Icons.Default.AddAPhoto,
                    label   = "Mostrar sección fotos",
                    detail  = "Cámara integrada en el formulario",
                    checked = prefs.showPhotos,
                    onToggle = vm::setShowPhotos,
                )
                Div()
                SwitchRow(
                    icon    = Icons.Default.CheckCircle,
                    label   = "Resultado obligatorio",
                    detail  = "No permite guardar sin seleccionar resultado",
                    checked = prefs.requireResult,
                    onToggle = vm::setRequireResult,
                )
            }

            // ── Notificaciones ────────────────────────────────
            SectionTitle("Notificaciones")
            InfoCard {
                SwitchRow(
                    icon    = Icons.Default.Notifications,
                    label   = "Notificaciones push",
                    detail  = "Avisos de nuevas rutas y mensajes",
                    checked = prefs.pushEnabled,
                    onToggle = vm::setPushEnabled,
                )
                Div()
                SwitchRow(
                    icon    = Icons.Default.Sync,
                    label   = "Sincronización automática",
                    detail  = "Sincroniza cada 15 min en segundo plano",
                    checked = prefs.autoSync,
                    onToggle = vm::setAutoSync,
                )
                Div()
                SwitchRow(
                    icon    = Icons.Default.Alarm,
                    label   = "Recordatorio de jornada",
                    detail  = if (prefs.jornadaReminder)
                        "Avisa a las ${"%02d".format(prefs.jornadaReminderHour)}:00"
                    else "Desactivado",
                    checked = prefs.jornadaReminder,
                    onToggle = vm::setJornadaReminder,
                )
                if (prefs.jornadaReminder) {
                    Div()
                    HourPickerRow(
                        hour    = prefs.jornadaReminderHour,
                        onMinus = { vm.setJornadaReminderHour(prefs.jornadaReminderHour - 1) },
                        onPlus  = { vm.setJornadaReminderHour(prefs.jornadaReminderHour + 1) },
                    )
                }
            }

            // ── Navegación ────────────────────────────────────
            SectionTitle("Navegación")
            InfoCard {
                InfoRowNav(
                    icon   = Icons.Default.CalendarMonth,
                    label  = "Calendario",
                    detail = "Planificación mensual de rutas",
                    onClick = onNavigateToCalendario,
                )
                if (ui.role in listOf("owner", "admin", "god")) {
                    Div()
                    InfoRowNav(
                        icon   = Icons.Default.AdminPanelSettings,
                        label  = "Admin",
                        detail = "Gestión de usuarios y cuenta",
                        onClick = onNavigateToAdmin,
                    )
                }
            }

            // ── Sesión ────────────────────────────────────────
            SectionTitle("Sesión")
            Button(
                onClick  = vm::onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Default.Logout, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Cerrar sesión")
            }

            Text(
                "RutasApp ${ui.appVersion}",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = Spacing.sm),
            )
        }
    }

    // ── Diálogo logout ────────────────────────────────────────
    if (ui.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = vm::onLogoutDismiss,
            icon  = { Icon(Icons.Default.Logout, null) },
            title = { Text("Cerrar sesión") },
            text  = { Text("¿Seguro que quieres cerrar sesión?") },
            confirmButton = {
                Button(
                    onClick = { vm.onLogoutConfirm(onLoggedOut) },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = vm::onLogoutDismiss) { Text("Cancelar") }
            },
        )
    }

    if (ui.isLoggingOut) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

// ── Componentes privados ──────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style      = MaterialTheme.typography.labelSmall,
        color      = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier   = Modifier.padding(top = Spacing.sm),
    )
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = Spacing.xs), content = content)
    }
}

@Composable
private fun Div() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InfoRowNav(icon: ImageVector, label: String, detail: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick  = onClick,
        color    = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = Spacing.lg, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(detail, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(
    icon:     ImageVector,
    label:    String,
    detail:   String,
    checked:  Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier          = Modifier.fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(detail, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun HourPickerRow(hour: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Schedule, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.md))
        Text("Hora del recordatorio",
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f))
        IconButton(onClick = onMinus, enabled = hour > 5) {
            Icon(Icons.Default.Remove, "Restar hora")
        }
        Text(
            "%02d:00".format(hour),
            style    = MaterialTheme.typography.titleSmall,
            color    = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(52.dp),
        )
        IconButton(onClick = onPlus, enabled = hour < 22) {
            Icon(Icons.Default.Add, "Sumar hora")
        }
    }
}

@Composable
private fun LanguageRow(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Language, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.md))
        Text("Idioma",
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f))
        Row(
            modifier              = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            listOf("es" to "ES", "en" to "EN").forEach { (code, label) ->
                FilterChip(
                    selected = current == code,
                    onClick  = { onSelect(code) },
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@Composable
private fun ThemeRow(currentMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, "Sistema", Icons.Default.PhoneAndroid),
        Triple(ThemeMode.LIGHT,  "Claro",   Icons.Default.LightMode),
        Triple(ThemeMode.DARK,   "Oscuro",  Icons.Default.DarkMode),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Palette, null, Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.md))
            Text("Apariencia",
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f))
        }
        Row(
            modifier              = Modifier.fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.sm)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            options.forEach { (mode, label, icon) ->
                FilterChip(
                    selected    = currentMode == mode,
                    onClick     = { onSelect(mode) },
                    label       = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
                    modifier    = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RolChip(role: String) {
    val (color, label) = when (role) {
        "god"   -> MaterialTheme.colorScheme.primary to "Superadmin"
        "owner" -> MaterialTheme.colorScheme.primary to "Propietario"
        "admin"        -> MaterialTheme.colorScheme.secondary      to "Admin"
        "manager"      -> MaterialTheme.colorScheme.tertiary       to "Manager"
        "agent"        -> MaterialTheme.colorScheme.onSurfaceVariant to "Agente"
        else           -> MaterialTheme.colorScheme.onSurfaceVariant to role
    }
    SuggestionChip(
        onClick = {},
        label   = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors  = SuggestionChipDefaults.suggestionChipColors(labelColor = color),
    )
}

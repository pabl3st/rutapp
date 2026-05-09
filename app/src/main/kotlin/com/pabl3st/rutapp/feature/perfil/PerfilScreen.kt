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
import com.pabl3st.rutapp.data.local.entity.StopTagConfig
import com.pabl3st.rutapp.data.local.entity.TagCondition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Color
import java.util.UUID

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
            // ── Apariencia y región ───────────────────────────
            SectionTitle("Apariencia")
            InfoCard {
                ThemeRow(currentMode = themeMode, onSelect = themeVm::setTheme)
                Div()
                LanguageRow(current = prefs.language, onSelect = vm::setLanguage)
            }

            // ── Negocio y formulario de visita ────────────────
            SectionTitle("Negocio y visitas")
            InfoCard {
                // Tipo de negocio + KPIs → una sola entrada (evita duplicado)
                InfoRowNav(
                    icon   = Icons.Default.BusinessCenter,
                    label  = "Negocio y KPIs",
                    detail = ui.sectorLabel,
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

            // ── Conexión y datos ──────────────────────────────
            SectionTitle("Conexión y datos")
            InfoCard {
                SwitchRow(
                    icon    = Icons.Default.Sync,
                    label   = "Sincronización automática",
                    detail  = "Sincroniza cada 15 min en segundo plano",
                    checked = prefs.autoSync,
                    onToggle = vm::setAutoSync,
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

            // ── Tags configurables (solo owner/admin/god) ─────
            if (ui.role in listOf("owner", "admin", "god")) {
                SectionTitle("Etiquetas de paradas")
                StopTagsSection(
                    tags              = prefs.stopTags,
                    kpiThreshold      = prefs.kpiThreshold,
                    onTagsChange      = vm::setStopTags,
                    onThresholdChange = vm::setKpiThreshold,
                )
            }

            // ── Accesos directos ──────────────────────────────
            SectionTitle("Accesos")
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
                        label  = "Administración",
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


// ══════════════════════════════════════════════════════════════
// CONFIGURADOR DE TAGS DE PARADAS
// Solo visible para owner / admin / god
// ══════════════════════════════════════════════════════════════

private val TAG_ICON_OPTIONS = listOf(
    "Star" to Icons.Default.Star,
    "Warning" to Icons.Default.Warning,
    "CheckCircle" to Icons.Default.CheckCircle,
    "Info" to Icons.Default.Info,
    "ThumbUp" to Icons.Default.ThumbUp,
    "ThumbDown" to Icons.Default.ThumbDown,
    "Store" to Icons.Default.Store,
    "TrendingUp" to Icons.Default.TrendingUp,
    "TrendingDown" to Icons.Default.TrendingDown,
    "Schedule" to Icons.Default.Schedule,
    "Flag" to Icons.Default.Flag,
    "LocalOffer" to Icons.Default.LocalOffer,
    "Block" to Icons.Default.Block,
    "Bolt" to Icons.Default.Bolt,
)

private val TAG_COLOR_PRESETS = listOf(
    "#dcfce7" to "#15803d",
    "#fee2e2" to "#dc2626",
    "#dbeafe" to "#1d4ed8",
    "#fef9c3" to "#a16207",
    "#f3e8ff" to "#7e22ce",
    "#ffedd5" to "#c2410c",
    "#e0f2fe" to "#0369a1",
    "#f1f5f9" to "#475569",
)

private fun TagCondition.label(): String = when (this) {
    TagCondition.ALWAYS           -> "Siempre"
    TagCondition.STATUS_DONE      -> "Stop visitado"
    TagCondition.STATUS_PENDING   -> "Stop pendiente"
    TagCondition.PDV_OPEN         -> "PDV abierto (última visita)"
    TagCondition.PDV_CLOSED       -> "PDV cerrado (última visita)"
    TagCondition.RESULT_IS        -> "Resultado ="
    TagCondition.KPI_ABOVE        -> "KPI > umbral"
    TagCondition.KPI_BELOW        -> "KPI < umbral"
    TagCondition.KPI_BOOL_TRUE    -> "KPI booleano = true"
    TagCondition.DAYS_SINCE_VISIT -> "Días sin visita ≥ umbral"
}

@Composable
private fun StopTagsSection(
    tags: List<StopTagConfig>,
    kpiThreshold: Double,
    onTagsChange: (List<StopTagConfig>) -> Unit,
    onThresholdChange: (Double) -> Unit,
) {
    var expandedTagId by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        // Umbral global
        InfoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Umbral de KPI", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Usado en condiciones KPI > umbral y KPI < umbral",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onThresholdChange((kpiThreshold - 1).coerceAtLeast(0.0)) },
                        modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Default.Remove, null, Modifier.size(16.dp)) }
                    Text(
                        kpiThreshold.toInt().toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.widthIn(min = 32.dp),
                    )
                    IconButton(
                        onClick = { onThresholdChange(kpiThreshold + 1) },
                        modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
                }
            }
        }

        // Lista de tags
        tags.forEachIndexed { idx, tag ->
            TagEditorRow(
                tag = tag,
                expanded = expandedTagId == tag.id,
                onExpand = { expandedTagId = if (expandedTagId == tag.id) null else tag.id },
                onChange = { updated -> onTagsChange(tags.toMutableList().also { it[idx] = updated }) },
                onDelete = { onTagsChange(tags.toMutableList().also { it.removeAt(idx) }) },
            )
        }

        // Añadir nuevo
        OutlinedButton(
            onClick = {
                val t = StopTagConfig(
                    id = UUID.randomUUID().toString(),
                    name = "Nueva etiqueta",
                    icon = "Label",
                    colorHex = "#f1f5f9",
                    textColorHex = "#475569",
                    condition = TagCondition.ALWAYS,
                )
                onTagsChange(tags + t)
                expandedTagId = t.id
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text("Añadir etiqueta")
        }
    }
}

@Composable
private fun TagEditorRow(
    tag: StopTagConfig,
    expanded: Boolean,
    onExpand: () -> Unit,
    onChange: (StopTagConfig) -> Unit,
    onDelete: () -> Unit,
) {
    val bgColor = runCatching {
        Color(android.graphics.Color.parseColor(tag.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.surfaceVariant)
    val textColor = runCatching {
        Color(android.graphics.Color.parseColor(tag.textColorHex))
    }.getOrDefault(MaterialTheme.colorScheme.onSurface)
    val iconVec = TAG_ICON_OPTIONS.firstOrNull { it.first == tag.icon }?.second
        ?: Icons.Default.Label

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Fila resumen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpand)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = bgColor,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(iconVec, null, Modifier.size(12.dp), tint = textColor)
                        Text(tag.name, style = MaterialTheme.typography.labelSmall, color = textColor)
                    }
                }
                Text(
                    tag.condition.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = tag.enabled,
                    onCheckedChange = { onChange(tag.copy(enabled = it)) },
                    modifier = Modifier.height(24.dp),
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Editor expandido
            if (expanded) {
                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Nombre
                    OutlinedTextField(
                        value = tag.name,
                        onValueChange = { onChange(tag.copy(name = it)) },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Condición
                    Text("Condición de aparición",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    var condExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = condExpanded,
                        onExpandedChange = { condExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = tag.condition.label(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Condición") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(condExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = condExpanded,
                            onDismissRequest = { condExpanded = false },
                        ) {
                            TagCondition.entries.forEach { cond ->
                                DropdownMenuItem(
                                    text = { Text(cond.label()) },
                                    onClick = {
                                        onChange(tag.copy(condition = cond))
                                        condExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Valor extra según condición
                    when (tag.condition) {
                        TagCondition.RESULT_IS -> OutlinedTextField(
                            value = tag.conditionValue ?: "",
                            onValueChange = { onChange(tag.copy(conditionValue = it.ifBlank { null })) },
                            label = { Text("Resultado esperado (ej: contactado)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TagCondition.KPI_ABOVE, TagCondition.KPI_BELOW, TagCondition.KPI_BOOL_TRUE -> {
                            OutlinedTextField(
                                value = tag.conditionKpiId ?: "",
                                onValueChange = { onChange(tag.copy(conditionKpiId = it.ifBlank { null })) },
                                label = { Text("ID del KPI (ej: telco_activaciones)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        TagCondition.DAYS_SINCE_VISIT -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("Días mínimos:", style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { onChange(tag.copy(conditionThreshold = (tag.conditionThreshold - 1).coerceAtLeast(1.0))) },
                                    modifier = Modifier.size(32.dp),
                                ) { Icon(Icons.Default.Remove, null, Modifier.size(16.dp)) }
                                Text(tag.conditionThreshold.toInt().toString(),
                                    style = MaterialTheme.typography.titleMedium)
                                IconButton(
                                    onClick = { onChange(tag.copy(conditionThreshold = tag.conditionThreshold + 1)) },
                                    modifier = Modifier.size(32.dp),
                                ) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
                            }
                        }
                        else -> Unit
                    }

                    // Icono
                    Text("Icono", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TAG_ICON_OPTIONS.size) { i ->
                            val (iName, iVec) = TAG_ICON_OPTIONS[i]
                            val sel = tag.icon == iName
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (sel) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { onChange(tag.copy(icon = iName)) }
                                    .then(if (sel) Modifier.border(2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.shapes.small) else Modifier),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(iVec, null, Modifier.size(18.dp),
                                        tint = if (sel) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Color
                    Text("Color", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TAG_COLOR_PRESETS.size) { i ->
                            val (bg, fg) = TAG_COLOR_PRESETS[i]
                            val sel = tag.colorHex == bg
                            val c = runCatching {
                                Color(android.graphics.Color.parseColor(bg))
                            }.getOrDefault(Color.Gray)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(c, MaterialTheme.shapes.small)
                                    .border(
                                        if (sel) 3.dp else 1.dp,
                                        if (sel) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        MaterialTheme.shapes.small,
                                    )
                                    .clickable { onChange(tag.copy(colorHex = bg, textColorHex = fg)) },
                            )
                        }
                    }

                    // Eliminar
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Eliminar etiqueta")
                    }
                }
            }
        }
    }
}

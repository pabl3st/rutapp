@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.perfil

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
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity

@Composable
fun BusinessProfileScreen(
    onBack: () -> Unit,
    vm: BusinessProfileViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil de negocio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = vm::onShowAddKpiDialog) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir KPI")
                    }
                }
            )
        }
    ) { padding ->
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Sector ────────────────────────────────────────
            item {
                SectorCard(
                    sector      = ui.profile?.sector ?: "custom",
                    sectorLabel = vm.sectorLabel(ui.profile?.sector ?: "custom"),
                    onChangeSector = vm::onShowSectorPicker,
                )
            }

            // ── KPIs comunes ──────────────────────────────────
            val commonKpis = ui.kpis.filter { it.sector == "common" }
            if (commonKpis.isNotEmpty()) {
                item {
                    Text(
                        "KPIs comunes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(commonKpis, key = { it.id }) { kpi ->
                    KpiRow(kpi = kpi, onToggle = { vm.onToggleKpiVisible(kpi.id, it) }, onDelete = null)
                }
            }

            // ── KPIs del sector ───────────────────────────────
            val sectorKpis = ui.kpis.filter { it.sector != "common" && it.isSystem }
            if (sectorKpis.isNotEmpty()) {
                item {
                    Text(
                        vm.sectorLabel(ui.profile?.sector ?: "custom"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(sectorKpis, key = { it.id }) { kpi ->
                    KpiRow(kpi = kpi, onToggle = { vm.onToggleKpiVisible(kpi.id, it) }, onDelete = null)
                }
            }

            // ── KPIs custom del usuario ───────────────────────
            val customKpis = ui.kpis.filter { !it.isSystem }
            if (customKpis.isNotEmpty()) {
                item {
                    Text(
                        "KPIs personalizados",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(customKpis, key = { it.id }) { kpi ->
                    KpiRow(
                        kpi      = kpi,
                        onToggle = { vm.onToggleKpiVisible(kpi.id, it) },
                        onDelete = { vm.deleteCustomKpi(kpi.id) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ── Selector de sector ────────────────────────────────────
    if (ui.showSectorPicker) {
        SectorPickerDialog(
            currentSector = ui.profile?.sector ?: "custom",
            sectors       = vm.sectors,
            sectorLabel   = vm::sectorLabel,
            onSelect      = vm::onSelectSector,
            onDismiss     = vm::onDismissSectorPicker,
        )
    }

    // ── Diálogo nuevo KPI custom ──────────────────────────────
    if (ui.showAddKpiDialog) {
        AddKpiDialog(ui = ui, vm = vm)
    }
}

@Composable
private fun SectorCard(sector: String, sectorLabel: String, onChangeSector: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Business,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Sector de negocio", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(sectorLabel, style = MaterialTheme.typography.titleMedium)
            }
            FilledTonalButton(onClick = onChangeSector) {
                Text("Cambiar")
            }
        }
    }
}

@Composable
private fun KpiRow(
    kpi: KpiDefinitionEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val typeIcon = when (kpi.type) {
        "number"  -> Icons.Default.Numbers
        "boolean" -> Icons.Default.ToggleOn
        "select"  -> Icons.Default.List
        else      -> Icons.Default.TextFields
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (kpi.visible) MaterialTheme.colorScheme.surface
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(typeIcon, contentDescription = null, modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(kpi.label, style = MaterialTheme.typography.bodyMedium,
                        color = if (kpi.visible) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (kpi.required) {
                        SuggestionChip(onClick = {}, label = { Text("Requerido",
                            style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(20.dp))
                    }
                }
                kpi.unit?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = kpi.visible, onCheckedChange = onToggle)
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar KPI",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SectorPickerDialog(
    currentSector: String,
    sectors: List<String>,
    sectorLabel: (String) -> String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elige tu sector") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sectors.forEach { sector ->
                    val selected = sector == currentSector
                    Card(
                        onClick = { onSelect(sector) },
                        colors  = CardDefaults.cardColors(
                            containerColor = if (selected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                        ),
                        border = if (selected) CardDefaults.outlinedCardBorder() else null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(sectorLabel(sector), style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                            if (selected) Icon(Icons.Default.Check, null, Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AddKpiDialog(ui: BusinessProfileUiState, vm: BusinessProfileViewModel) {
    val types    = listOf("number", "boolean", "select", "text")
    val sections = listOf("general", "objetivos", "pedidos", "notas")

    AlertDialog(
        onDismissRequest = vm::onDismissAddKpiDialog,
        title = { Text("Nuevo KPI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value         = ui.newKpiLabel,
                    onValueChange = vm::onNewKpiLabelChange,
                    label         = { Text("Nombre *") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    isError       = ui.error != null,
                    supportingText = ui.error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                )
                OutlinedTextField(
                    value         = ui.newKpiUnit,
                    onValueChange = vm::onNewKpiUnitChange,
                    label         = { Text("Unidad (opcional)") },
                    placeholder   = { Text("€, ud, %, días…") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                // Tipo
                Text("Tipo", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = ui.newKpiType == t,
                            onClick  = { vm.onNewKpiTypeChange(t) },
                            label    = { Text(t, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                // Sección
                Text("Sección", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sections.forEach { s ->
                        FilterChip(
                            selected = ui.newKpiSection == s,
                            onClick  = { vm.onNewKpiSectionChange(s) },
                            label    = { Text(s, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Requerido", style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                    Switch(checked = ui.newKpiRequired, onCheckedChange = vm::onNewKpiRequiredChange)
                }
            }
        },
        confirmButton = {
            Button(onClick = vm::saveCustomKpi) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = vm::onDismissAddKpiDialog) { Text("Cancelar") }
        }
    )
}




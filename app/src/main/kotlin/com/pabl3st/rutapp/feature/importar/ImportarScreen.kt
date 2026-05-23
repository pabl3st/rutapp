@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.importar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Checkbox
import com.pabl3st.rutapp.data.network.AccountUserDto
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.importer.GeoCluster
import com.pabl3st.rutapp.core.ui.theme.Spacing
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ImportarScreen(
    onBack: () -> Unit = {},
    onDone: () -> Unit = {},
    vm: ImportarViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Importar paradas") },
                navigationIcon = {
                    IconButton(onClick = if (ui.step == ImportStep.DONE) onDone else onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Stepper visual ────────────────────────────────
            StepIndicator(
                current = ui.step,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )

            HorizontalDivider()

            when (ui.step) {
                ImportStep.PICK_FILE   -> StepPickFile(ui = ui, vm = vm)
                ImportStep.MAP_COLUMNS -> StepMapColumns(ui = ui, vm = vm)
                ImportStep.PREVIEW     -> StepPreview(ui = ui, vm = vm)
                ImportStep.CALENDAR    -> StepCalendar(ui = ui, vm = vm)
                ImportStep.KPI_REPORTS -> StepKpiReports(ui = ui, vm = vm)
                ImportStep.DONE        -> StepDone(ui = ui, onDone = onDone)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Indicador de pasos
// ─────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(current: ImportStep, modifier: Modifier = Modifier) {
    val steps = listOf("Fichero", "Columnas", "Preview", "Calendario", "KPIs", "Listo")
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { idx, label ->
            val state = when {
                idx < current.ordinal  -> StepState.DONE
                idx == current.ordinal -> StepState.ACTIVE
                else                   -> StepState.PENDING
            }
            StepDot(label = label, state = state, modifier = Modifier.weight(1f))
            if (idx < steps.size - 1) {
                HorizontalDivider(
                    modifier  = Modifier.weight(1f),
                    color     = if (state == StepState.DONE) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                    thickness = 2.dp,
                )
            }
        }
    }
}

private enum class StepState { DONE, ACTIVE, PENDING }

@Composable
private fun StepDot(label: String, state: StepState, modifier: Modifier = Modifier) {
    val color = when (state) {
        StepState.DONE    -> MaterialTheme.colorScheme.primary
        StepState.ACTIVE  -> MaterialTheme.colorScheme.primary
        StepState.PENDING -> MaterialTheme.colorScheme.outlineVariant
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = if (state == StepState.PENDING) MaterialTheme.colorScheme.surface else color,
            modifier = Modifier
                .size(24.dp)
                .then(if (state == StepState.PENDING) Modifier.border(1.dp, color, MaterialTheme.shapes.extraSmall) else Modifier),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (state == StepState.DONE) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// ─────────────────────────────────────────────────────────────
// PASO 1 — Seleccionar fichero
// ─────────────────────────────────────────────────────────────

@Composable
private fun StepPickFile(ui: ImportarUiState, vm: ImportarViewModel) {
    var pendingUri: Pair<Uri, String>? by remember { mutableStateOf(null) }

    // Selector de asignado en cascada según rol del caller
    if (ui.availableAgents.isNotEmpty() || ui.isLoadingAgents) {
        AssigneePickerCascade(
            ui      = ui,
            onSelf        = { vm.onSelectTargetUser(null) },
            onSelectAdmin   = vm::onSelectAdmin,
            onSelectManager = vm::onSelectManager,
            onSelectAgent   = vm::onSelectTargetUser,
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // nombre del fichero lo leemos del URI
            pendingUri = uri to (uri.lastPathSegment ?: "fichero.csv")
        }
    }

    // Disparar parseo cuando tenemos URI
    LaunchedEffect(pendingUri) {
        pendingUri?.let { (uri, name) ->
            vm.onFilePicked(uri, name)
            pendingUri = null
        }
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(Spacing.lg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.UploadFile,
            null,
            modifier = Modifier.size(72.dp),
            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(Spacing.lg))
        Text("Importar desde CSV o Excel", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Selecciona un fichero .csv, .txt o .xlsx con tus PDVs.\n" +
            "La primera fila debe ser la cabecera.\n" +
            "CSV: separadores coma, punto y coma, tabulador.\n" +
            "Excel: se lee la primera hoja.",
            style   = MaterialTheme.typography.bodyMedium,
            color   = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        if (ui.fileName != null) {
            Spacer(Modifier.height(Spacing.md))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier          = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(ui.fileName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        ui.parseError?.let { err ->
            Spacer(Modifier.height(Spacing.md))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        if (ui.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick  = { launcher.launch(arrayOf(
                    "text/csv",
                    "text/plain",
                    "text/tab-separated-values",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel",
                    "*/*",
                )) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.FolderOpen, null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Seleccionar fichero CSV / XLSX")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PASO 2 — Mapear columnas
// ─────────────────────────────────────────────────────────────

@Composable
private fun StepMapColumns(ui: ImportarUiState, vm: ImportarViewModel) {
    val NONE = "(no importar)"
    val options = listOf(NONE) + ui.csvHeaders

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier        = Modifier.weight(1f),
            contentPadding  = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Text(
                    "Indica qué columna del CSV corresponde a cada campo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            StopField.entries.forEach { field ->
                item(key = field.name) {
                    ColumnMappingRow(
                        field   = field,
                        options = options,
                        current = ui.mapping[field] ?: NONE,
                        onSelect = { selected ->
                            vm.onMappingChange(field, if (selected == NONE) null else selected)
                        },
                    )
                }
            }
            ui.mappingError?.let { err ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.padding(Spacing.lg).fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = { vm.onMappingConfirm(vm.getRawRows()) }) {
                Text("Continuar")
                Spacer(Modifier.width(Spacing.sm))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ColumnMappingRow(
    field:    StopField,
    options:  List<String>,
    current:  String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                field.label,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (field.required) {
                Text(
                    "Obligatorio",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        ExposedDropdownMenuBox(
            expanded         = expanded,
            onExpandedChange = { expanded = it },
            modifier         = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value          = current,
                onValueChange  = {},
                readOnly       = true,
                trailingIcon   = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier       = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                singleLine     = true,
                textStyle      = MaterialTheme.typography.bodySmall,
            )
            ExposedDropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text    = { Text(opt, style = MaterialTheme.typography.bodySmall) },
                        onClick = { onSelect(opt); expanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PASO 3 — Vista previa + clustering
// ─────────────────────────────────────────────────────────────

@Composable
private fun StepPreview(ui: ImportarUiState, vm: ImportarViewModel) {
    var showClusterConfig by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier        = Modifier.weight(1f),
            contentPadding  = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Resumen
            item {
                SummaryBanner(
                    total    = ui.previews.size,
                    withGps  = ui.previews.count { it.hasGps },
                    warnings = ui.previews.count { it.warning != null },
                    routes   = ui.clusters.size,
                )
            }

            // Configuración de clustering
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountTree, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.sm))
                            Text("Agrupación en rutas", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { showClusterConfig = !showClusterConfig }) {
                                Text(if (showClusterConfig) "Ocultar" else "Configurar")
                            }
                        }
                        if (showClusterConfig) {
                            Spacer(Modifier.height(Spacing.sm))
                            ClusterConfigPanel(params = ui.clusterParams, onUpdate = vm::onClusterParamsChange)
                        }
                    }
                }
            }

            // Rutas generadas con sus paradas
            ui.clusters.forEachIndexed { clusterIdx, stops ->
                item(key = "header_$clusterIdx") {
                    ClusterHeader(
                        index    = clusterIdx,
                        name     = ui.clusterNames.getOrElse(clusterIdx) { "Ruta ${clusterIdx + 1}" },
                        count    = stops.size,
                        onNameChange = { vm.onClusterNameChange(clusterIdx, it) },
                    )
                }
                itemsIndexed(stops, key = { _, s -> "${clusterIdx}_${s.rowIndex}" }) { stopIdx, stop ->
                    StopPreviewCard(stop = stop, index = stopIdx)
                }
            }

            item { Spacer(Modifier.height(Spacing.xl)) }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.padding(Spacing.lg).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.End),
        ) {
            if (ui.isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress        = { if (ui.saveTotal > 0) ui.saveProgress.toFloat() / ui.saveTotal else 0f },
                        modifier        = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "${ui.saveProgress}/${ui.saveTotal} paradas guardadas…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Button(
                    onClick  = vm::onPreviewConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = ui.clusters.isNotEmpty(),
                ) {
                    Text("Siguiente — Asignar fechas")
                    Spacer(Modifier.width(Spacing.sm))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryBanner(total: Int, withGps: Int, warnings: Int, routes: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier              = Modifier.padding(Spacing.md).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryItem(value = "$total", label = "Paradas", icon = Icons.Default.Place)
            SummaryItem(value = "$withGps", label = "Con GPS", icon = Icons.Default.GpsFixed)
            SummaryItem(value = "$routes", label = "Rutas", icon = Icons.Default.Route)
            if (warnings > 0) {
                SummaryItem(value = "$warnings", label = "Avisos", icon = Icons.Default.Warning)
            }
        }
    }
}

@Composable
private fun SummaryItem(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun ClusterConfigPanel(params: ClusterParams, onUpdate: (ClusterParams) -> Unit) {
    val strategies = listOf(
        GeoCluster.Strategy.AUTO    to "Auto (1 ruta / 15 stops)",
        GeoCluster.Strategy.FIXED_K to "Número fijo de rutas",
        GeoCluster.Strategy.RADIUS  to "Por radio (km)",
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        strategies.forEach { (s, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(selected = params.strategy == s, onClick = { onUpdate(params.copy(strategy = s)) })
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
        when (params.strategy) {
            GeoCluster.Strategy.FIXED_K -> {
                OutlinedTextField(
                    value         = params.fixedK.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { onUpdate(params.copy(fixedK = it.coerceIn(1, 50))) } },
                    label         = { Text("Número de rutas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                )
            }
            GeoCluster.Strategy.RADIUS -> {
                OutlinedTextField(
                    value         = params.radiusKm.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdate(params.copy(radiusKm = it.coerceIn(0.1, 500.0))) } },
                    label         = { Text("Radio máximo (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                )
            }
            else -> {}
        }
        // Fecha de inicio
        Spacer(Modifier.height(Spacing.xs))
        Text("Fecha de primera ruta", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(onClick = { onUpdate(params.copy(startDate = params.startDate.minusDays(1))) }) {
                Icon(Icons.Default.ChevronLeft, null, Modifier.size(18.dp))
            }
            Text(
                params.startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es"))),
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            OutlinedButton(onClick = { onUpdate(params.copy(startDate = params.startDate.plusDays(1))) }) {
                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ClusterHeader(index: Int, name: String, count: Int, onNameChange: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                "${index + 1}",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (editing) {
            OutlinedTextField(
                value         = name,
                onValueChange = onNameChange,
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                textStyle     = MaterialTheme.typography.bodyMedium,
                trailingIcon  = {
                    IconButton(onClick = { editing = false }) {
                        Icon(Icons.Default.Done, null, Modifier.size(18.dp))
                    }
                },
            )
        } else {
            Text(
                name,
                style    = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { editing = true }) {
                Icon(Icons.Default.Edit, "Renombrar", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                "$count paradas",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun StopPreviewCard(stop: StopPreview, index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (stop.warning != null)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier          = Modifier.padding(Spacing.sm).padding(start = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${index + 1}",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stop.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    stop.externalId?.let { id ->
                        Spacer(Modifier.width(4.dp))
                        Text("[$id]", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                stop.address?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                stop.warning?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Icon(
                if (stop.hasGps) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                null,
                tint     = if (stop.hasGps) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PASO 4 — Asignación de rutas al calendario
// ─────────────────────────────────────────────────────────────

@Composable
private fun StepCalendar(ui: ImportarUiState, vm: ImportarViewModel) {
    val fmtDay   = DateTimeFormatter.ofPattern("dd MMM", java.util.Locale("es"))
    val fmtMonth = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale("es"))
    val now      = java.time.YearMonth.now()

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // ── Header: fechas del import o selector de mes ─────
        item {
            // Selector de mes — SIEMPRE visible
            // Si hay hoja CALENDARIO las fechas vienen del import pero el usuario puede cambiar el mes
            Text("Mes de importación", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                if (ui.hasCalendarSheet)
                    "Fechas del fichero pre-rellenadas. Cambia el mes si necesitas ajustarlas."
                else
                    "Sin fechas en el fichero — asigna el mes y las fechas manualmente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = { vm.onMonthChange(ui.selectedMonth.minusMonths(1)) }) {
                    Icon(Icons.Default.ChevronLeft, null)
                }
                Text(
                    ui.selectedMonth.atDay(1).format(fmtMonth).replaceFirstChar { it.uppercase() },
                    style     = MaterialTheme.typography.titleMedium,
                    modifier  = Modifier.widthIn(min = 160.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { vm.onMonthChange(ui.selectedMonth.plusMonths(1)) }) {
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
            if (ui.hasCalendarSheet) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null,
                        Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Fechas del fichero cargadas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
            Text(
                "${ui.calendarEntries.size} rutas · ${ui.calendarEntries.sumOf { it.stopCount }} paradas totales",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        // ── Barra de selección masiva ─────────────────────
        item {
            MultiSelectBar(
                totalCount    = ui.calendarEntries.size,
                selectedCount = ui.selectedCalendarIndices.size,
                bulkDate      = ui.bulkDate,
                onSelectAll   = vm::onSelectAllCalendarEntries,
                onClear       = vm::onClearCalendarSelection,
                onDateChange  = vm::onBulkDateChange,
                onApply       = vm::onApplyBulkDate,
                fmtDay        = fmtDay,
            )
        }

        // ── Una card por ruta ────────────────────────────────
        itemsIndexed(ui.calendarEntries) { idx, entry ->
            val isSelected = idx in ui.selectedCalendarIndices
            RouteCalendarCard(
                entry          = entry,
                selectedMonth  = ui.selectedMonth,
                fmtDay         = fmtDay,
                isSelected     = isSelected,
                onToggleSelect = { vm.onToggleCalendarEntry(idx) },
                onDateChange   = { date -> vm.onCalendarDateChange(idx, date) },
                onAddDate      = { vm.onCalendarAddDate(idx, it) },
                onRemoveDate   = { vm.onCalendarRemoveDate(idx, it) },
            )
        }

        item {
            Spacer(Modifier.height(Spacing.sm))
            // Mostrar saveError si existe
            ui.saveError?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(err, modifier = Modifier.padding(Spacing.md),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(Spacing.sm))
            }
            Button(
                onClick  = vm::onCalendarConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val hasKpi = ui.hasKpiSheet || ui.kpiHeaders.isNotEmpty()
                Text(if (hasKpi) "Siguiente — KPIs y visitas" else "Importar")
                Spacer(Modifier.width(Spacing.sm))
                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
            }
            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick  = vm::skipCalendarStep,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Saltar — usar fechas por defecto") }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Card de ruta en el paso Calendario
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteCalendarCard(
    entry:          com.pabl3st.rutapp.feature.importar.RouteCalendarEntry,
    selectedMonth:  java.time.YearMonth,
    fmtDay:         java.time.format.DateTimeFormatter,
    isSelected:     Boolean = false,
    onToggleSelect: () -> Unit = {},
    onDateChange:   (LocalDate) -> Unit,
    onAddDate:      (LocalDate) -> Unit,
    onRemoveDate:   (LocalDate) -> Unit,
) {
    // Fechas del mes seleccionado
    val datesThisMonth = entry.scheduledDates
        .filter { java.time.YearMonth.from(it) == selectedMonth }

    // Sin fecha asignada → mostrar estado de advertencia
    val sinFecha = entry.date == null && !entry.datesFromImport && datesThisMonth.isEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        border   = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = if (sinFecha)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
        else
            CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {

            // ── Cabecera: nombre + nº paradas ────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Checkbox de selección masiva
                Checkbox(
                    checked         = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier        = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.routeName,
                        style    = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${entry.stopCount} paradas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (entry.datesFromImport) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            "Del fichero",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Sin fecha: advertencia + selector de día ──────
            if (sinFecha) {
                Text(
                    "⚠ Sin fecha — selecciona al menos una fecha de visita",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(6.dp))
            }

            // ── Chips de fechas ya asignadas ──────────────────
            if (datesThisMonth.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp),
                ) {
                    datesThisMonth.forEach { d ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                modifier              = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                            ) {
                                Text(
                                    d.format(fmtDay),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                // No borrar si vienen del fichero
                                if (!entry.datesFromImport) {
                                    Spacer(Modifier.width(2.dp))
                                    IconButton(
                                        onClick  = { onRemoveDate(d) },
                                        modifier = Modifier.size(16.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Close, null,
                                            Modifier.size(10.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else if (entry.datesFromImport) {
                // Tiene fechas del fichero pero no del mes seleccionado
                Text(
                    "No hay visitas en el mes seleccionado para esta ruta",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Selector de fecha: ±día + botón "Añadir fecha" ─
            if (!entry.datesFromImport) {
                val pivot = entry.date ?: LocalDate.now()
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Navegar al día anterior
                    OutlinedIconButton(
                        onClick  = { onDateChange(pivot.minusDays(1)) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.ChevronLeft, null, Modifier.size(16.dp))
                    }
                    // Fecha pivot actual
                    Surface(
                        shape    = MaterialTheme.shapes.small,
                        color    = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            pivot.format(fmtDay),
                            style     = MaterialTheme.typography.labelMedium,
                            modifier  = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    // Navegar al día siguiente
                    OutlinedIconButton(
                        onClick  = { onDateChange(pivot.plusDays(1)) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp))
                    }
                    // Añadir esta fecha al array de scheduledDates
                    FilledTonalButton(
                        onClick  = { onAddDate(pivot) },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Añadir", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PASO 5 — KPIs y reports históricos de visitas
// ─────────────────────────────────────────────────────────────

@Composable
private fun StepKpiReports(ui: ImportarUiState, vm: ImportarViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("KPIs y visitas históricas", style = MaterialTheme.typography.titleSmall)
        Text(
            "Mapea las columnas de KPIs de tu fichero para importar el historial de visitas de cada parada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (ui.kpiHeaders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        "No se detectó hoja de KPIs en el fichero",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        "Para importar KPIs históricos, añade una hoja llamada KPI_VISITAS o CSV_KPI_VISITAS con columnas: stop_id, date, kpi_activaciones, kpi_primer_bono, etc.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        } else {
            // Mapeo de columnas KPI
            KpiField.entries.forEach { field ->
                val currentHeader = ui.kpiMapping[field]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            field.label + if (field.required) " *" else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick   = { expanded = true },
                            modifier  = Modifier.width(180.dp),
                        ) {
                            Text(
                                currentHeader ?: "— no mapear —",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style    = MaterialTheme.typography.labelSmall,
                            )
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text    = { Text("— no mapear —") },
                                onClick = { vm.onKpiMappingChange(field, null); expanded = false }
                            )
                            ui.kpiHeaders.forEach { h ->
                                DropdownMenuItem(
                                    text    = { Text(h, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = { vm.onKpiMappingChange(field, h); expanded = false }
                                )
                            }
                        }
                    }
                }
            }
            ui.kpiMappingError?.let { err ->
                Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        // Botones
        Button(
            onClick  = if (ui.kpiHeaders.isEmpty()) vm::onSkipKpi else vm::onKpiMappingConfirm,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (ui.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(Spacing.sm))
                Text("Importando... ${ui.saveProgress}/${ui.saveTotal}")
            } else {
                Icon(Icons.Default.CloudUpload, null)
                Spacer(Modifier.width(Spacing.sm))
                Text(if (ui.kpiHeaders.isEmpty()) "Importar sin KPIs" else "Importar con KPIs")
            }
        }
        if (ui.kpiHeaders.isNotEmpty()) {
            OutlinedButton(
                onClick  = vm::onSkipKpi,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Saltar KPIs — importar solo paradas y rutas")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PASO 6 — Importación completada
// ─────────────────────────────────────────────────────────────

@Composable
private fun StepDone(ui: ImportarUiState, onDone: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CheckCircle,
            null,
            modifier = Modifier.size(80.dp),
            tint     = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text("Importación completada", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "${ui.previews.size} paradas importadas en ${ui.clusters.size} rutas.\n" +
            "Puedes verlas en la pantalla de Rutas.",
            style   = MaterialTheme.typography.bodyMedium,
            color   = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        ui.saveError?.let { err ->
            Spacer(Modifier.height(Spacing.md))
            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(Spacing.xl))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Route, null)
            Spacer(Modifier.width(Spacing.sm))
            Text("Ver mis rutas")
        }
    }
}





// ─────────────────────────────────────────────────────────────
// AssigneePickerCascade — selector jerárquico owner→admin→manager→agent
// ─────────────────────────────────────────────────────────────
@Composable
private fun AssigneePickerCascade(
    ui:             ImportarUiState,
    onSelf:         () -> Unit,
    onSelectAdmin:   (AccountUserDto?) -> Unit,
    onSelectManager: (AccountUserDto?) -> Unit,
    onSelectAgent:   (AccountUserDto?) -> Unit,
) {
    val callerRole = ui.callerRole  // "owner","admin","manager","god"
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    ) {
        Text(
            "Asignar rutas a:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))

        // Opción "para mí mismo" siempre primera
        FilterChip(
            selected    = ui.targetUser == null && ui.selectedAdmin == null && ui.selectedManager == null,
            onClick     = { onSelf(); onSelectAdmin(null); onSelectManager(null) },
            label       = { Text("Para mí mismo") },
            leadingIcon = if (ui.targetUser == null && ui.selectedAdmin == null) {
                { Icon(Icons.Default.Person, null, Modifier.size(16.dp)) }
            } else null,
        )

        if (ui.isLoadingAgents) {
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Text("Cargando equipo…", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // ── Nivel 1: Admin (solo visible para owner/god) ──────────
            if (ui.hierarchyAdmins.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text("Admin", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                ui.hierarchyAdmins.forEach { admin ->
                    val sel = ui.selectedAdmin?.userId == admin.userId
                    FilterChip(
                        selected = sel,
                        onClick  = { onSelectAdmin(admin); onSelectManager(null) },
                        label    = { Text(admin.displayName) },
                        leadingIcon = if (sel) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }

            // ── Nivel 2: Manager (visible cuando hay managers en scope) ─
            if (ui.hierarchyManagers.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text("Manager", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                ui.hierarchyManagers.forEach { manager ->
                    val sel = ui.selectedManager?.userId == manager.userId
                    FilterChip(
                        selected = sel,
                        onClick  = { onSelectManager(manager) },
                        label    = { Text(manager.displayName) },
                        leadingIcon = if (sel) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }

            // ── Nivel 3: Agent ────────────────────────────────────────
            if (ui.hierarchyAgents.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text("Agente", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                ui.hierarchyAgents.forEach { agent ->
                    val sel = ui.targetUser?.userId == agent.userId
                    FilterChip(
                        selected = sel,
                        onClick  = { onSelectAgent(agent) },
                        label    = {
                            Column {
                                Text(agent.displayName,
                                    style = MaterialTheme.typography.labelMedium)
                                if (agent.role != "agent") Text(agent.role,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        leadingIcon = if (sel) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }

            // ── Resumen de selección ──────────────────────────────────
            val summary = when {
                ui.targetUser != null ->
                    "Rutas para: ${ui.targetUser!!.displayName} (${ui.targetUser!!.role})"
                ui.selectedManager != null ->
                    "Equipo de ${ui.selectedManager!!.displayName} — elige un agente"
                ui.selectedAdmin != null ->
                    "Cuenta de ${ui.selectedAdmin!!.displayName} — elige manager/agente"
                else -> null
            }
            summary?.let {
                Spacer(Modifier.height(Spacing.xs))
                Text(it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
    }
}

// ─────────────────────────────────────────────────────────────
// MultiSelectBar — barra de selección masiva en el paso Calendario
// ─────────────────────────────────────────────────────────────
@Composable
private fun MultiSelectBar(
    totalCount:    Int,
    selectedCount: Int,
    bulkDate:      java.time.LocalDate?,
    onSelectAll:   () -> Unit,
    onClear:       () -> Unit,
    onDateChange:  (java.time.LocalDate) -> Unit,
    onApply:       () -> Unit,
    fmtDay:        java.time.format.DateTimeFormatter,
) {
    if (totalCount == 0) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (selectedCount == 0) "Selección masiva"
                    else "$selectedCount / $totalCount rutas seleccionadas",
                    style     = MaterialTheme.typography.labelMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    if (selectedCount < totalCount) {
                        OutlinedButton(
                            onClick       = onSelectAll,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier      = Modifier.height(32.dp),
                        ) { Text("Todas", style = MaterialTheme.typography.labelSmall) }
                    }
                    if (selectedCount > 0) {
                        OutlinedButton(
                            onClick       = onClear,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier      = Modifier.height(32.dp),
                        ) { Text("Limpiar", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }

            // Selector de fecha + aplicar — solo visible si hay selección
            if (selectedCount > 0) {
                Spacer(Modifier.height(Spacing.sm))
                HorizontalDivider()
                Spacer(Modifier.height(Spacing.sm))
                Text("Aplicar misma fecha a las ${selectedCount} rutas seleccionadas:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    val pivot = bulkDate ?: java.time.LocalDate.now()
                    OutlinedIconButton(
                        onClick  = { onDateChange(pivot.minusDays(1)) },
                        modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Default.ChevronLeft, null, Modifier.size(16.dp)) }
                    Surface(
                        shape    = MaterialTheme.shapes.small,
                        color    = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            pivot.format(fmtDay),
                            style    = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    OutlinedIconButton(
                        onClick  = { onDateChange(pivot.plusDays(1)) },
                        modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp)) }
                    FilledTonalButton(
                        onClick        = onApply,
                        modifier       = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Icon(Icons.Default.Done, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Aplicar", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

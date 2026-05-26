package com.pabl3st.rutapp.feature.rutas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.data.local.entity.StopEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStopsScreen(
    onBack: () -> Unit,
    vm: AddStopsViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(ui.done) { if (ui.done) onBack() }
    LaunchedEffect(ui.error) {
        ui.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Añadir paradas")
                        if (ui.selected.isNotEmpty()) {
                            Text("${ui.selected.size} seleccionadas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Seleccionar todo
                    if (ui.stops.isNotEmpty()) {
                        IconButton(onClick = {
                            if (ui.selected.size == ui.stops.size) vm.onClearSelection()
                            else vm.onSelectAll()
                        }) {
                            Icon(
                                if (ui.selected.size == ui.stops.size) Icons.Default.Deselect
                                else Icons.Default.SelectAll,
                                contentDescription = "Seleccionar todo",
                            )
                        }
                    }
                    // Ordenar
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, "Ordenar")
                        }
                        DropdownMenu(expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }) {
                            listOf(
                                AddStopsSortMode.NAME        to "Por nombre",
                                AddStopsSortMode.POSTAL_CODE to "Por código postal",
                                AddStopsSortMode.LOCALITY    to "Por localidad",
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { vm.onSortChange(mode); showSortMenu = false },
                                    leadingIcon = {
                                        if (ui.sortMode == mode && !ui.groupByProxim)
                                            Icon(Icons.Default.Check, null,
                                                tint = MaterialTheme.colorScheme.primary)
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Agrupar por cercanía") },
                                onClick = { vm.onToggleProximity(); showSortMenu = false },
                                leadingIcon = {
                                    if (ui.groupByProxim)
                                        Icon(Icons.Default.Check, null,
                                            tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (ui.selected.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick  = vm::confirmAdd,
                        enabled  = !ui.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        if (ui.isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Añadir ${ui.selected.size} parada${if (ui.selected.size != 1) "s" else ""}")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Buscador
            OutlinedTextField(
                value         = ui.query,
                onValueChange = vm::onQueryChange,
                placeholder   = { Text("Nombre, ID, CP, localidad…") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = if (ui.query.isNotEmpty()) ({
                    IconButton(onClick = { vm.onQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Limpiar")
                    }
                }) else null,
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Indicador de ordenación activa
            if (ui.groupByProxim || ui.sortMode != AddStopsSortMode.NAME) {
                val label = if (ui.groupByProxim) "Agrupado por cercanía"
                else when (ui.sortMode) {
                    AddStopsSortMode.POSTAL_CODE -> "Ordenado por CP"
                    AddStopsSortMode.LOCALITY    -> "Ordenado por localidad"
                    else -> ""
                }
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Default.FilterList, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                ui.stops.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text(if (ui.query.isBlank()) "No hay paradas en la biblioteca"
                             else "Sin resultados para \"${ui.query}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(ui.stops, key = { it.uid }) { stop ->
                        val isSelected = stop.uid in ui.selected
                        AddStopCard(
                            stop       = stop,
                            isSelected = isSelected,
                            onToggle   = { vm.onToggleSelect(stop.uid) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddStopCard(
    stop:       StopEntity,
    isSelected: Boolean,
    onToggle:   () -> Unit,
) {
    val cp       = extractPostalCode(stop.address)
    val locality = extractLocality(stop.address)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.shapes.medium,
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Checkbox de selección
            Checkbox(
                checked  = isSelected,
                onCheckedChange = { onToggle() },
            )
            // Datos del PDV
            Column(Modifier.weight(1f)) {
                // ID externo + nombre
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    stop.externalId?.let { extId ->
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(extId,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Text(stop.name,
                        style    = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                // CP + Localidad
                if (cp.isNotEmpty() || locality.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (cp.isNotEmpty()) {
                            Text(cp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (locality.isNotEmpty()) {
                            Text(locality,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

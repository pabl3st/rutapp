@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.rutas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing

@Composable
fun EditarParadaScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    vm: EditarParadaViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.saved) {
        if (ui.saved) onSaved()
    }

    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar parada") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (ui.isSaving) {
                        CircularProgressIndicator(Modifier.size(24.dp).padding(end = 4.dp))
                    } else {
                        TextButton(onClick = vm::save, enabled = !ui.isLoading) {
                            Text("Guardar")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // ── Identificación ───────────────────────────────
            SectionLabel("Identificación")
            OutlinedTextField(
                value         = ui.name,
                onValueChange = vm::onNameChange,
                label         = { Text("Nombre *") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                isError       = ui.error != null && ui.name.isBlank(),
            )
            OutlinedTextField(
                value         = ui.externalId,
                onValueChange = vm::onExternalIdChange,
                label         = { Text("ID externo (código cliente)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // ── Ubicación ────────────────────────────────────
            SectionLabel("Ubicación")
            OutlinedTextField(
                value         = ui.address,
                onValueChange = vm::onAddressChange,
                label         = { Text("Dirección") },
                modifier      = Modifier.fillMaxWidth(),
                trailingIcon  = {
                    if (ui.isGeocoding) {
                        CircularProgressIndicator(Modifier.size(18.dp))
                    } else {
                        IconButton(onClick = vm::geocodeAddress, enabled = ui.address.isNotBlank()) {
                            Icon(Icons.Default.MyLocation, "Geocodificar")
                        }
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value         = ui.lat,
                    onValueChange = vm::onLatChange,
                    label         = { Text("Latitud") },
                    modifier      = Modifier.weight(1f),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value         = ui.lng,
                    onValueChange = vm::onLngChange,
                    label         = { Text("Longitud") },
                    modifier      = Modifier.weight(1f),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            // ── Contacto ─────────────────────────────────────
            SectionLabel("Contacto")
            OutlinedTextField(
                value         = ui.contactName,
                onValueChange = vm::onContactNameChange,
                label         = { Text("Nombre del contacto") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                leadingIcon   = { Icon(Icons.Default.Person, null, Modifier.size(18.dp)) },
            )
            OutlinedTextField(
                value         = ui.contactPhone,
                onValueChange = vm::onContactPhoneChange,
                label         = { Text("Teléfono") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon   = { Icon(Icons.Default.Phone, null, Modifier.size(18.dp)) },
            )
            OutlinedTextField(
                value         = ui.openingHours,
                onValueChange = vm::onOpeningHoursChange,
                label         = { Text("Horario de apertura") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                leadingIcon   = { Icon(Icons.Default.Schedule, null, Modifier.size(18.dp)) },
                placeholder   = { Text("Ej: L-V 9:00-18:00") },
            )

            // ── Clasificación ────────────────────────────────
            SectionLabel("Clasificación")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value         = ui.segment,
                    onValueChange = vm::onSegmentChange,
                    label         = { Text("Segmento") },
                    modifier      = Modifier.weight(1f),
                    singleLine    = true,
                    placeholder   = { Text("A / B / C") },
                )
                OutlinedTextField(
                    value         = ui.visitFrequency,
                    onValueChange = vm::onVisitFrequencyChange,
                    label         = { Text("Frec. visita (días)") },
                    modifier      = Modifier.weight(1f),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            // Prioridad con slider
            Column {
                Text("Prioridad: ${ui.priority}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value         = ui.priority.toFloat(),
                    onValueChange = { vm.onPriorityChange(it.toInt()) },
                    valueRange    = 1f..5f,
                    steps         = 3,
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Alta", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Baja", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Notas ────────────────────────────────────────
            SectionLabel("Notas")
            OutlinedTextField(
                value         = ui.notes,
                onValueChange = vm::onNotesChange,
                label         = { Text("Notas internas") },
                modifier      = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                maxLines      = 4,
            )

            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

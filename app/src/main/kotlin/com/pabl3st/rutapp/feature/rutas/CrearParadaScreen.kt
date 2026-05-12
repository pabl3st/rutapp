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

@Composable
fun CrearParadaScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    vm: CrearParadaViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navegar atrás cuando se guarda correctamente
    LaunchedEffect(ui.savedUid) {
        if (ui.savedUid != null) onSaved()
    }

    // Mostrar error en snackbar
    LaunchedEffect(ui.error) {
        ui.error?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva parada") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (ui.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        TextButton(onClick = vm::save) {
                            Text("Guardar")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Identificación ────────────────────────────────
            SectionHeader("Identificación")

            OutlinedTextField(
                value         = ui.name,
                onValueChange = vm::onNameChange,
                label         = { Text("Nombre *") },
                placeholder   = { Text("Ej: Farmacia Central") },
                singleLine    = true,
                isError       = ui.error != null && ui.name.isBlank(),
                modifier      = Modifier.fillMaxWidth(),
                leadingIcon   = { Icon(Icons.Default.Store, contentDescription = null) },
            )

            OutlinedTextField(
                value         = ui.externalId,
                onValueChange = vm::onExternalIdChange,
                label         = { Text("Código cliente") },
                placeholder   = { Text("Ej: LCC00237") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                leadingIcon   = { Icon(Icons.Default.Tag, contentDescription = null) },
            )

            // ── Ubicación ─────────────────────────────────────
            SectionHeader("Ubicación")

            OutlinedTextField(
                value         = ui.address,
                onValueChange = vm::onAddressChange,
                label         = { Text("Dirección") },
                placeholder   = { Text("Calle y número, ciudad") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                leadingIcon   = { Icon(Icons.Default.Place, contentDescription = null) },
                trailingIcon  = {
                    if (ui.isGeocoding) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = vm::geocodeAddress) {
                            Icon(
                                imageVector        = Icons.Default.MyLocation,
                                contentDescription = "Obtener coordenadas",
                                tint               = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = ui.lat,
                    onValueChange = vm::onLatChange,
                    label         = { Text("Latitud") },
                    placeholder   = { Text("39.4699") },
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value         = ui.lng,
                    onValueChange = vm::onLngChange,
                    label         = { Text("Longitud") },
                    placeholder   = { Text("-0.3763") },
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            if (ui.lat.isNotBlank() && ui.lng.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text  = "Coordenadas establecidas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // ── Contacto ──────────────────────────────────────
            SectionHeader("Contacto")

            OutlinedTextField(
                value         = ui.contactName,
                onValueChange = vm::onContactNameChange,
                label         = { Text("Persona de contacto") },
                placeholder   = { Text("Nombre del responsable") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                leadingIcon   = { Icon(Icons.Default.Person, contentDescription = null) },
            )

            OutlinedTextField(
                value         = ui.contactPhone,
                onValueChange = vm::onContactPhoneChange,
                label         = { Text("Teléfono") },
                placeholder   = { Text("+34 600 000 000") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                leadingIcon   = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )

            // ── Prioridad ─────────────────────────────────────
            SectionHeader("Prioridad")

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (1..5).forEach { p ->
                    val selected = ui.priority == p
                    FilterChip(
                        selected = selected,
                        onClick  = { vm.onPriorityChange(p) },
                        label    = { Text("$p") },
                        modifier = Modifier.weight(1f),
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (p) {
                                1, 2 -> MaterialTheme.colorScheme.errorContainer
                                4, 5 -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                        ),
                    )
                }
            }
            Text(
                text  = when (ui.priority) {
                    1 -> "Prioridad máxima"
                    2 -> "Prioridad alta"
                    3 -> "Prioridad normal"
                    4 -> "Prioridad baja"
                    else -> "Prioridad mínima"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── Notas ─────────────────────────────────────────
            SectionHeader("Notas")

            OutlinedTextField(
                value         = ui.notes,
                onValueChange = vm::onNotesChange,
                label         = { Text("Observaciones") },
                placeholder   = { Text("Horario especial, acceso, observaciones...") },
                modifier      = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                maxLines      = 4,
            )

            // ── Botón guardar ─────────────────────────────────
            Button(
                onClick  = vm::save,
                enabled  = !ui.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (ui.isSaving) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(20.dp),
                        color     = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar parada")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text  = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

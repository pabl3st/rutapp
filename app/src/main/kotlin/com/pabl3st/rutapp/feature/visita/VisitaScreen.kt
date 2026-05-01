@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.visita

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val VISIT_RESULTS = listOf(
    Triple("contactado",  "Contactado",    Icons.Default.CheckCircle),
    Triple("no_estaba",   "No estaba",     Icons.Default.PersonOff),
    Triple("volvemos",    "Volvemos",      Icons.Default.Replay),
    Triple("rechazado",   "Rechazado",     Icons.Default.Cancel),
)

@Composable
fun VisitaScreen(
    stopUid: String,
    onBack: () -> Unit,
    vm: VisitaViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // Volver automáticamente al guardar
    LaunchedEffect(ui.saved) {
        if (ui.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.stop?.name ?: "Visita") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick  = vm::saveVisit,
                    enabled  = !ui.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (ui.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Guardar visita")
                }
            }
        }
    ) { padding ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            ui.stop == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Parada no encontrada", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                val stop = ui.stop!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {

                    // ── Info del stop ──────────────────────────────
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            stop.externalId?.let { extId ->
                                Text(
                                    text  = extId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(stop.name, style = MaterialTheme.typography.titleMedium)
                            stop.address?.let { addr ->
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, null, Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(addr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            stop.contactName?.let { contact ->
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(contact,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // ── Resultado de la visita ─────────────────────
                    Text(
                        "Resultado",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VISIT_RESULTS.forEach { (value, label, icon) ->
                            val selected = ui.selectedResult == value
                            val containerColor = if (selected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface

                            Card(
                                onClick = { vm.onResultChange(value) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                                border = if (selected)
                                    androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                    ) else null,
                            ) {
                                Row(
                                    modifier          = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (selected) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text  = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (selected) {
                                        Spacer(Modifier.weight(1f))
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Notas ──────────────────────────────────────
                    OutlinedTextField(
                        value         = ui.notes,
                        onValueChange = vm::onNotesChange,
                        label         = { Text("Notas de la visita") },
                        placeholder   = { Text("Observaciones, incidencias...") },
                        modifier      = Modifier.fillMaxWidth(),
                        minLines      = 3,
                        maxLines      = 6,
                    )

                    // ── Próxima acción ─────────────────────────────
                    OutlinedTextField(
                        value         = ui.nextAction,
                        onValueChange = vm::onNextActionChange,
                        label         = { Text("Próxima acción") },
                        placeholder   = { Text("Qué hacer en la siguiente visita...") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = false,
                        minLines      = 2,
                        maxLines      = 4,
                        leadingIcon   = {
                            Icon(Icons.Default.NextPlan, null, Modifier.size(18.dp))
                        },
                    )

                    // Espacio para el bottomBar
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

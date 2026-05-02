@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun JornadaBar(
    routeUid: String,
    modifier: Modifier = Modifier,
    vm: JornadaViewModel = hiltViewModel(),
) {
    LaunchedEffect(routeUid) { vm.init(routeUid) }

    val ui by vm.ui.collectAsStateWithLifecycle()
    val session = ui.session
    val state   = session?.state ?: "idle"

    Surface(
        modifier      = modifier.fillMaxWidth(),
        color         = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Timer
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = if (state == "idle") "Jornada" else vm.formatElapsed(ui.elapsedMs),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily  = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                    ),
                    color = when (state) {
                        "running" -> MaterialTheme.colorScheme.primary
                        "paused"  -> MaterialTheme.colorScheme.tertiary
                        "done"    -> MaterialTheme.colorScheme.secondary
                        else      -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (state != "idle") {
                    Text(
                        text  = "%.1f km".format(ui.distanceKm),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Botones de control
            when (state) {
                "idle", "paused" -> {
                    FilledTonalIconButton(
                        onClick = { if (state == "idle") vm.start() else vm.resume() },
                        colors  = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar jornada")
                    }
                    if (state == "paused") {
                        FilledTonalIconButton(onClick = vm::finish) {
                            Icon(Icons.Default.FlagCircle, contentDescription = "Finalizar jornada")
                        }
                    }
                }
                "running" -> {
                    FilledTonalIconButton(
                        onClick = vm::pause,
                        colors  = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pausar jornada")
                    }
                    FilledTonalIconButton(onClick = vm::finish) {
                        Icon(Icons.Default.FlagCircle, contentDescription = "Finalizar jornada")
                    }
                }
                "done" -> {
                    SuggestionChip(
                        onClick = {},
                        label   = { Text("Finalizada", style = MaterialTheme.typography.labelSmall) },
                        icon    = {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor       = MaterialTheme.colorScheme.secondary,
                            iconContentColor = MaterialTheme.colorScheme.secondary,
                        ),
                    )
                }
            }
        }
    }
}

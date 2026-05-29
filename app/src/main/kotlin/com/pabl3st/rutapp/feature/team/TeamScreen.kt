@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.network.AgentOverviewDto

/**
 * Lista del equipo: agentes con estado de jornada, progreso de stops hoy y KPIs del mes.
 * Visible para: manager (sus agentes directos), admin/owner/god (todos los agentes).
 * Polling cada 60s para actualizar estado GPS y jornada.
 */
@Composable
fun TeamScreen(
    onBack:           () -> Unit,
    onAgentDetail:    (Int) -> Unit,
    /** Drill-down: pulsar admin/manager navega a un nuevo TeamScreen del
     *  subárbol de ese usuario. Si null, solo se navega a AgentDetail. */
    onTeamDrillDown:  ((userId: Int, userName: String) -> Unit)? = null,
    vm: TeamViewModel = hiltViewModel(),
) {
    val ui      by vm.ui.collectAsStateWithLifecycle()
    val agents  by vm.filteredAgents.collectAsStateWithLifecycle()
    val snack   = remember { SnackbarHostState() }

    LaunchedEffect(ui.error) {
        ui.error?.let { snack.showSnackbar(it); vm.clearError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // Drill-down: muestra "Equipo de XXX". Normal: "Mi equipo".
                        Text(ui.viewAsUserName?.let { "Equipo de $it" } ?: "Mi equipo")
                        if (ui.agents.isNotEmpty()) {
                            val active = ui.agents.count { it.isActive }
                            Text("${ui.agents.size} personas · $active activas ahora",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Filtro: solo activos
                    FilterChip(
                        selected = ui.filterActive,
                        onClick  = vm::toggleActiveFilter,
                        label    = { Text("En ruta", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (ui.filterActive) ({
                            Icon(Icons.Default.Check, null, Modifier.size(14.dp))
                        }) else null,
                        modifier = Modifier.padding(end = Spacing.sm),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = ui.isRefreshing,
            onRefresh    = vm::refresh,
            modifier     = Modifier.padding(padding),
        ) {
            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text("Cargando equipo...", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                agents.isEmpty() -> Box(Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Group, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text(if (ui.filterActive) "Nadie en ruta ahora"
                             else "Sin agentes en el equipo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(agents, key = { it.userId }) { agent ->
                        AgentCard(
                            agent   = agent,
                            // Drill-down: si el usuario pulsado es admin/manager
                            // navegamos al TeamScreen de su subárbol. Si es agent
                            // (caso terminal de la jerarquía), navegamos al detalle.
                            onClick = {
                                val isLeaf = agent.role == "agent" || agent.role == "viewer"
                                if (isLeaf || onTeamDrillDown == null) {
                                    onAgentDetail(agent.userId)
                                } else {
                                    onTeamDrillDown(agent.userId, agent.name)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentCard(agent: AgentOverviewDto, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = androidx.compose.foundation.BorderStroke(
            0.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(Spacing.md)) {

            // ── Cabecera: avatar + nombre + estado jornada ─────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Avatar iniciales
                Surface(
                    shape  = MaterialTheme.shapes.extraLarge,
                    color  = when {
                        agent.isActive -> MaterialTheme.colorScheme.primaryContainer
                        agent.isPaused -> MaterialTheme.colorScheme.secondaryContainer
                        agent.isDone   -> MaterialTheme.colorScheme.tertiaryContainer
                        else           -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            agent.name.take(2).uppercase(),
                            style     = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                agent.isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                                agent.isPaused -> MaterialTheme.colorScheme.onSecondaryContainer
                                agent.isDone   -> MaterialTheme.colorScheme.onTertiaryContainer
                                else           -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(agent.name, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text("@${agent.username}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Estado de jornada
                val (stateLabel, stateColor) = when {
                    agent.isActive -> "En ruta" to MaterialTheme.colorScheme.primary
                    agent.isPaused -> "Pausado" to MaterialTheme.colorScheme.tertiary
                    agent.isDone   -> "Finalizado" to MaterialTheme.colorScheme.secondary
                    else           -> "Sin jornada" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = stateColor.copy(alpha = 0.12f),
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        if (agent.isActive) {
                            Icon(Icons.Default.FiberManualRecord, null,
                                Modifier.size(8.dp), tint = stateColor)
                        }
                        Text(stateLabel, style = MaterialTheme.typography.labelSmall,
                            color = stateColor)
                    }
                }
            }

            // ── Progreso de hoy ───────────────────────────────
            if (agent.stopsTotal > 0) {
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    LinearProgressIndicator(
                        progress       = { agent.progressToday },
                        modifier       = Modifier.weight(1f).height(4.dp),
                        trackColor     = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        "${agent.stopsDone}/${agent.stopsTotal}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (agent.stopsDone == agent.stopsTotal)
                            MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Stats rápidas ─────────────────────────────────
            val hasStats = agent.stopsTotal > 0 || agent.monthDone > 0 ||
                           (agent.distanceKm > 0.1)
            if (hasStats) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    if (agent.distanceKm > 0.1) {
                        StatChip(Icons.Default.Route, "%.1f km".format(agent.distanceKm))
                    }
                    if (agent.stopsContacted > 0) {
                        StatChip(Icons.Default.CheckCircle, "${agent.stopsContacted} contactados")
                    }
                    if (agent.monthDone > 0) {
                        StatChip(Icons.Default.CalendarMonth,
                            "${agent.monthDone} este mes")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

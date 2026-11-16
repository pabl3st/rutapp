package com.pabl3st.rutapp.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.network.AgentOverviewDto

/**
 * Pestaña "Hoy" del manager/admin/owner: estado del equipo de un vistazo.
 *  1. Tres contadores (visitas de hoy, en ruta, sin iniciar)
 *  2. Alertas (gestores con ruta hoy y jornada sin iniciar)
 *  3. Una tarjeta por persona del equipo con progreso y estado
 * Los datos salen de team_overview (estado por FECHA, no por espejo del stop).
 */
fun LazyListScope.managerHoyItems(
    team:         List<AgentOverviewDto>,
    loading:      Boolean,
    onAgentClick: (AgentOverviewDto) -> Unit,
    filter:       String = "todos",
    onFilter:     (String) -> Unit = {},
    nudgedIds:    Set<Int> = emptySet(),
    onNudge:      (AgentOverviewDto) -> Unit = {},
    showMap:      Boolean = false,
    onShowMap:    (Boolean) -> Unit = {},
    liveMap:      @Composable (List<AgentOverviewDto>) -> Unit = {},
) {
    val withRoute = team.filter { it.stopsTotal > 0 }
    val done      = team.sumOf { it.stopsDone }
    val total     = team.sumOf { it.stopsTotal }
    val inRoute   = team.count { it.isActive || it.isPaused }
    val notStarted = withRoute.filter { it.jornadaState == null }

    item(key = "mh_strip") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            HoyTile("$done/$total", "Visitas hoy", Modifier.weight(1f))
            HoyTile("$inRoute", "En ruta", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            HoyTile("${notStarted.size}", "Sin iniciar", Modifier.weight(1f),
                if (notStarted.isEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.tertiary)
        }
    }

    if (notStarted.isNotEmpty()) {
        item(key = "mh_alert_title") { HoySectionLabel("REQUIERE ATENCIÓN") }
        items(notStarted, key = { "mh_alert_${it.userId}" }) { a ->
            Card(
                onClick = { onAgentClick(a) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Row(
                    Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Column(Modifier.weight(1f)) {
                        Text("${a.name} no ha iniciado la jornada",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("${a.stopsTotal} paradas asignadas hoy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    if (a.userId in nudgedIds) {
                        Text("✓ Enviado", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    } else {
                        FilledTonalButton(onClick = { onNudge(a) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Icon(Icons.Default.NotificationsActive, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Avisar")
                        }
                    }
                }
            }
        }
    }

    item(key = "mh_team_title") { HoySectionLabel("EQUIPO HOY — ${team.size} ${if (team.size == 1) "persona" else "personas"}") }

    item(key = "mh_view") {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !showMap, onClick = { onShowMap(false) },
                shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}) { Text("Lista") }
            SegmentedButton(selected = showMap, onClick = { onShowMap(true) },
                shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}) { Text("Mapa en vivo") }
        }
    }
    if (showMap) {
        item(key = "mh_map") {
            Column {
                Box(Modifier.fillMaxWidth().height(340.dp)
                    .clip(MaterialTheme.shapes.large)) { liveMap(team) }
                Text("Posición actualizada cada 15 s · toca una persona para ver su detalle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }

    item(key = "mh_filters") {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(
                Triple("todos", "Todos", team.size),
                Triple("ruta",  "En ruta", inRoute),
                Triple("sin",   "Sin iniciar", notStarted.size),
            ).forEach { (k, l, n) ->
                FilterChip(selected = filter == k, onClick = { onFilter(k) }, label = { Text("$l $n") })
            }
        }
    }

    if (team.isEmpty()) {
        item(key = "mh_empty") {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    if (loading) "Cargando equipo…" else "Aún no tienes personas asignadas a tu equipo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.md),
                )
            }
        }
    } else {
        val shown = when (filter) {
            "ruta" -> team.filter { it.isActive || it.isPaused }
            "sin"  -> notStarted
            else   -> team
        }
        items(shown, key = { "mh_agent_${it.userId}" }) { a -> HoyAgentCard(a) { onAgentClick(a) } }
    }
}

@Composable
private fun HoySectionLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xs),
    )
}

@Composable
private fun HoyTile(
    value: String, label: String, modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Text(value, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun HoyAgentCard(a: AgentOverviewDto, onClick: () -> Unit) {
    val (stateLabel, stateColor) = when {
        a.isActive -> "En ruta"     to MaterialTheme.colorScheme.primary
        a.isPaused -> "En pausa"    to MaterialTheme.colorScheme.tertiary
        a.isDone   -> "Terminada"   to MaterialTheme.colorScheme.secondary
        a.stopsTotal > 0 -> "Sin iniciar" to MaterialTheme.colorScheme.tertiary
        else       -> "Sin ruta hoy" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Surface(shape = MaterialTheme.shapes.large, color = stateColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(a.name.take(2).uppercase(), style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold, color = stateColor)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(a.name, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val extra = buildList {
                        if (a.elapsedMs > 0 && (a.isActive || a.isPaused)) add(com.pabl3st.rutapp.core.formatDuration(a.elapsedMs))
                        if (a.distanceKm > 0) add(com.pabl3st.rutapp.core.formatKm(a.distanceKm))
                        if (a.lastLat != null) add("GPS activo")
                    }.joinToString(" · ")
                    Text(if (extra.isEmpty()) stateLabel else "$stateLabel · $extra",
                        style = MaterialTheme.typography.labelSmall, color = stateColor, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                Text("${a.stopsDone}/${a.stopsTotal}", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold)
            }
            if (a.stopsTotal > 0) {
                Spacer(Modifier.height(Spacing.sm))
                LinearProgressIndicator(
                    progress = { a.progressToday },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = stateColor,
                )
            }
        }
    }
}

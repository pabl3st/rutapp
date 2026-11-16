@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.pabl3st.rutapp.feature.calendario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pabl3st.rutapp.core.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Agenda semanal del supervisor: una fila por comercial, una columna por dia
 * laborable (L-V). Los huecos (comercial sin ruta un dia) se marcan en ambar.
 * Asignar: tocar una ruta de "Sin programar" y luego la casilla, o tocar una
 * casilla vacia para elegir la ruta.
 */
@Composable
fun AgendaSemana(
    ui:          CalendarioUiState,
    onPrevWeek:  () -> Unit,
    onNextWeek:  () -> Unit,
    onPick:      (String?) -> Unit,
    onCellTap:   (agentId: Int, day: LocalDate) -> Unit,
    onRouteClick:(String) -> Unit,
    modifier:    Modifier = Modifier,
) {
    val fmt  = DateTimeFormatter.ISO_LOCAL_DATE
    val days = (0..4).map { ui.weekStart.plusDays(it.toLong()) }
    val end  = days.last()
    val title = "${ui.weekStart.dayOfMonth} – ${end.dayOfMonth} " +
        end.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("es")))

    // Rutas sin programar: ninguna fecha desde hoy y no terminadas
    val todayStr = ui.today.format(fmt)
    val unscheduled = ui.allRoutes.filter { r ->
        r.status != "done" && r.status != "cancelled" &&
            (listOf(r.dateAssigned) + (r.scheduledDates ?: emptyList()))
                .none { it.isNotBlank() && it >= todayStr }
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.md)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevWeek) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Semana anterior") }
            Text(title, Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onNextWeek) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Semana siguiente") }
        }

        // Cabecera de días
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(64.dp))
            days.forEach { d ->
                val isToday = d == ui.today
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(d.format(DateTimeFormatter.ofPattern("EEE", Locale("es"))).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isToday) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                    ) {
                        Text("${d.dayOfMonth}", Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))

        var gaps = 0
        if (ui.agents.isEmpty()) {
            Text(
                if (ui.loadingAgents) "Cargando equipo…" else "No hay comerciales en tu equipo.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.lg),
            )
        }
        ui.agents.forEach { agent ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(agent.displayName.substringBefore(' '), Modifier.width(64.dp),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                days.forEach { d ->
                    val ds = d.format(fmt)
                    val rs = (ui.routesByDate[ds] ?: emptyList()).filter { it.userId == agent.userId }
                    val onVacation = ds in ui.vacationDays
                    Box(Modifier.weight(1f).padding(horizontal = 2.dp)) {
                        if (rs.isNotEmpty()) {
                            Card(Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(Modifier.fillMaxWidth().padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center) {
                                    rs.forEach { r ->
                                        Text(r.name, Modifier.clickable { onRouteClick(r.uid) },
                                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        } else {
                            val picking = ui.pickedRouteUid != null
                            val isGap = !picking && !onVacation
                            if (isGap) gaps++
                            val accent = when {
                                picking    -> MaterialTheme.colorScheme.primary
                                isGap      -> MaterialTheme.colorScheme.tertiary
                                else       -> MaterialTheme.colorScheme.outlineVariant
                            }
                            OutlinedCard(
                                onClick = { onCellTap(agent.userId, d) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                border = BorderStroke(if (picking || isGap) 1.5.dp else 1.dp, accent),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isGap) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                                                     else androidx.compose.ui.graphics.Color.Transparent),
                            ) {
                                Box(Modifier.fillMaxWidth().heightIn(min = 50.dp), contentAlignment = Alignment.Center) {
                                    if (onVacation) Text("Vac.", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    else Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = accent)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (ui.agents.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(if (gaps == 0) "Sin huecos esta semana" else "$gaps huecos: comercial sin ruta ese día",
                style = MaterialTheme.typography.labelMedium,
                color = if (gaps == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
        }

        // Bandeja: rutas sin programar
        Spacer(Modifier.height(Spacing.md))
        Text(if (ui.pickedRouteUid != null) "SIN PROGRAMAR — toca una casilla para asignarla" else "SIN PROGRAMAR — elige una ruta",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        if (unscheduled.isEmpty()) {
            Text("Todas las rutas tienen fecha ✔", style = MaterialTheme.typography.bodyMedium)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                unscheduled.forEach { r ->
                    FilterChip(
                        selected = ui.pickedRouteUid == r.uid,
                        onClick  = { onPick(if (ui.pickedRouteUid == r.uid) null else r.uid) },
                        label    = { Text(r.name) },
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

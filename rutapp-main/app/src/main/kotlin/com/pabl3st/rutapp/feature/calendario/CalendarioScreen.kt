@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarioScreen(
    onRouteClick: (String) -> Unit = {},
    vm: CalendarioViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Calendario") })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // ── Cabecera mes ──────────────────────────────────
            MonthHeader(
                month   = ui.currentMonth,
                onPrev  = vm::prevMonth,
                onNext  = vm::nextMonth,
            )

            // ── Grid de días ──────────────────────────────────
            DayOfWeekHeader()
            CalendarGrid(
                month          = ui.currentMonth,
                today          = ui.today,
                selectedDay    = ui.selectedDay,
                routesByDate   = ui.routesByDate,
                onDayClick     = vm::selectDay,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

            // ── Rutas del día seleccionado ────────────────────
            val dateLabel = ui.selectedDay?.let {
                it.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("es")))
                    .replaceFirstChar { c -> c.uppercase() }
            } ?: "Sin selección"

            Text(
                text     = dateLabel,
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            )

            when {
                ui.isLoading -> Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                ui.selectedRoutes.isEmpty() -> Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventBusy, null,
                            Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            "Sin rutas este día",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> LazyColumn(
                    modifier        = Modifier.fillMaxWidth().weight(1f),
                    contentPadding  = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(ui.selectedRoutes, key = { it.uid }) { route ->
                        CalendarioRouteCard(route = route, onClick = { onRouteClick(route.uid) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month:  YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, "Mes anterior")
        }
        Text(
            text  = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es")))
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, "Mes siguiente")
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    val days = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
    )
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs)) {
        days.forEach { dow ->
            Text(
                text      = dow.getDisplayName(TextStyle.NARROW, Locale("es")).uppercase(),
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month:        YearMonth,
    today:        LocalDate,
    selectedDay:  LocalDate?,
    routesByDate: Map<String, List<RouteEntity>>,
    onDayClick:   (LocalDate) -> Unit,
) {
    val fmt         = DateTimeFormatter.ISO_LOCAL_DATE
    val firstDay    = month.atDay(1)
    // Monday = 1, offset to align grid
    val startOffset = (firstDay.dayOfWeek.value - 1)
    val daysInMonth = month.lengthOfMonth()
    val totalCells  = startOffset + daysInMonth
    val rows        = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs)) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNum    = cellIndex - startOffset + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date    = month.atDay(dayNum)
                        val dateStr = date.format(fmt)
                        val routes  = routesByDate[dateStr] ?: emptyList()
                        val isToday    = date == today
                        val isSelected = date == selectedDay
                        val hasRoutes  = routes.isNotEmpty()
                        val allDone    = hasRoutes && routes.all { it.status == "done" }

                        Box(
                            modifier          = Modifier.weight(1f).aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday    -> MaterialTheme.colorScheme.primaryContainer
                                        else       -> androidx.compose.ui.graphics.Color.Transparent
                                    }
                                )
                                .clickable { onDayClick(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text  = "$dayNum",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday    -> MaterialTheme.colorScheme.primary
                                        else       -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                if (hasRoutes) {
                                    Box(
                                        modifier = Modifier.size(5.dp).clip(CircleShape).background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                allDone    -> MaterialTheme.colorScheme.secondary
                                                else       -> MaterialTheme.colorScheme.tertiary
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarioRouteCard(route: RouteEntity, onClick: () -> Unit) {
    val statusColor = when (route.status) {
        "active"    -> MaterialTheme.colorScheme.primary
        "done"      -> MaterialTheme.colorScheme.secondary
        "cancelled" -> MaterialTheme.colorScheme.error
        else        -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIcon = when (route.status) {
        "active"    -> Icons.Default.PlayCircle
        "done"      -> Icons.Default.CheckCircle
        "cancelled" -> Icons.Default.Cancel
        else        -> Icons.Default.Schedule
    }
    val statusLabel = when (route.status) {
        "active"    -> "Activa"
        "done"      -> "Completada"
        "cancelled" -> "Cancelada"
        else        -> "Pendiente"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick  = onClick,
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(statusIcon, null, Modifier.size(20.dp), tint = statusColor)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    route.name,
                    style    = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                route.notes?.let {
                    Text(
                        it,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
            )
        }
    }
}

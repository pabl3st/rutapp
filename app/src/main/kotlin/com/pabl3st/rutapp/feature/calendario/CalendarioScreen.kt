@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.calendario

import com.pabl3st.rutapp.core.ui.theme.RouteStatusTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarioScreen(
    onBack: () -> Unit = {},
    showBackButton: Boolean = false,
    onRouteClick: (String) -> Unit = {},
    vm: CalendarioViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(ui.snackbar) {
        ui.snackbar?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.clearSnackbar()
        }
    }

    Scaffold(


        modifier = Modifier.semantics { testTag = "calendario-screen" },
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Calendario") },
                navigationIcon = if (showBackButton) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                } else {
                    {}  // slot vacío — sin flecha atrás cuando es tab del BottomNav
                },
                actions = {
                    // Botón "Hoy" — solo visible si no estamos ya en el mes actual
                    if (ui.currentMonth != java.time.YearMonth.now()) {
                        TextButton(onClick = vm::goToToday) {
                            Icon(Icons.Default.Today, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hoy")
                        }
                    }
                },
            )
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
                holidays       = ui.holidays,
                vacationDays   = ui.vacationDays,
                onDayClick     = vm::selectDay,
                onDayLongPress = vm::onDayLongPress,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

            // ── Rutas del día seleccionado ────────────────────
            val dateLabel = ui.selectedDay?.let {
                it.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("es")))
                    .replaceFirstChar { c -> c.uppercase() }
            } ?: "Sin selección"

            val selectedHoliday = ui.selectedDay?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                ?.let { ui.holidays[it] }

            Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)) {
                Text(
                    text  = dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (selectedHoliday != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Celebration, null,
                            modifier = Modifier.size(12.dp),
                            tint     = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text  = selectedHoliday.localName.ifBlank { selectedHoliday.name },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                val selectedDateStr = ui.selectedDay?.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) ?: ""
                if (selectedDateStr in ui.vacationDays) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BeachAccess, null,
                            modifier = Modifier.size(12.dp),
                            tint     = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text  = "Vacaciones",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

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
                        Spacer(Modifier.height(Spacing.sm))
                        TextButton(onClick = {
                            // Simular pulsación larga sobre el día seleccionado para abrir menú
                            ui.selectedDay?.let { vm.onDayLongPress(it) }
                        }) {
                            Icon(Icons.Default.AddCircleOutline, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Asignar ruta", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                else -> LazyColumn(
                    modifier        = Modifier.fillMaxWidth().weight(1f),
                    contentPadding  = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(ui.selectedRoutes, key = { it.uid }) { route ->
                        CalendarioRouteCard(
                            route           = route,
                            onClick         = { onRouteClick(route.uid) },
                            onRemoveFromDay = { vm.onRemoveRouteFromDay(route) },
                        )
                    }
                }
            }
        }
    }

    // ── Menú contextual pulsación larga ────────────────────
    if (ui.showDayMenu) {
        val dayLabel = ui.menuDay?.let {
            it.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("es")))
                .replaceFirstChar { c -> c.uppercase() }
        } ?: ""
        val hasRoute = ui.menuDay?.let {
            ui.routesByDate[it.format(DateTimeFormatter.ISO_LOCAL_DATE)]?.isNotEmpty() == true
        } == true

        val menuDateStr = ui.menuDay?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""
        val isVacationDay = menuDateStr in ui.vacationDays

        AlertDialog(
            onDismissRequest = vm::dismissDayMenu,
            title = { Text(dayLabel, style = MaterialTheme.typography.titleSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Siempre mostrar "Asignar ruta" — permite añadir más rutas al día
                    TextButton(
                        onClick  = vm::onShowRouteSelector,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.AddCircleOutline, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (hasRoute) "Añadir otra ruta a este día" else "Asignar ruta a este día")
                    }
                    if (hasRoute) {
                        TextButton(
                            onClick  = vm::onRemoveRoute,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Quitar ruta de este día")
                        }
                    }
                    // Toggle vacaciones
                    TextButton(
                        onClick  = vm::onMarkVacation,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            if (isVacationDay) Icons.Default.EventAvailable else Icons.Default.BeachAccess,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isVacationDay) "Quitar vacaciones" else "Marcar como vacaciones",
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = vm::dismissDayMenu) { Text("Cerrar") }
            },
        )
    }

    // ── Selector de ruta para asignar al día ──────────────
    if (ui.showRouteSelector) {
        val dayLabel = ui.menuDay?.let {
            it.format(DateTimeFormatter.ofPattern("d MMMM", Locale("es")))
        } ?: ""

        // Todas las rutas son asignables a cualquier día sin restricción
        val assignableRoutes = ui.allRoutes

        AlertDialog(
            onDismissRequest = vm::onDismissRouteSelector,
            title = { Text("Asignar ruta al $dayLabel") },
            text = {
                if (assignableRoutes.isEmpty()) {
                    Text(
                        "No hay rutas disponibles. Crea una ruta primero desde la pantalla de Rutas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(assignableRoutes, key = { it.uid }) { route ->
                            Card(
                                onClick   = { vm.onAssignRouteToDay(route) },
                                modifier  = Modifier.fillMaxWidth(),
                                colors    = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                            ) {
                                Row(
                                    modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.Route,
                                        null,
                                        Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            route.name,
                                            style    = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        val dateCount = remember(route.scheduledDates, route.dateAssigned) {
                                            val dates = (route.scheduledDates ?: emptyList()).toMutableList()
                                            if (route.dateAssigned.isNotBlank() && route.dateAssigned != "1970-01-01"
                                                && !dates.contains(route.dateAssigned)) dates.add(0, route.dateAssigned)
                                            dates.size
                                        }
                                        Text(
                                            if (dateCount > 0) "$dateCount día${if (dateCount > 1) "s" else ""} asignado${if (dateCount > 1) "s" else ""}"
                                            else "Sin fechas asignadas",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (dateCount > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = vm::onDismissRouteSelector) { Text("Cancelar") }
            },
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarGrid(
    month:          YearMonth,
    today:          LocalDate,
    selectedDay:    LocalDate?,
    routesByDate:   Map<String, List<RouteEntity>>,
    holidays:       Map<String, PublicHoliday>,
    vacationDays:   Set<String>                    = emptySet(),
    onDayClick:     (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit = {},
) {
    val fmt         = DateTimeFormatter.ISO_LOCAL_DATE
    val firstDay    = month.atDay(1)
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
                        val date      = month.atDay(dayNum)
                        val dateStr   = date.format(fmt)
                        val routes    = routesByDate[dateStr] ?: emptyList()
                        val holiday   = holidays[dateStr]
                        val isToday    = date == today
                        val isSelected = date == selectedDay
                        val isWeekend  = date.dayOfWeek.value >= 6
                        val hasRoutes  = routes.isNotEmpty()
                        val allDone    = hasRoutes && routes.all { it.status == "done" }
                        val isVacation = dateStr in vacationDays

                        Box(
                            modifier          = Modifier.weight(1f)
                                .heightIn(min = 44.dp)
                                .padding(2.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(
                                    when {
                                        isSelected  -> MaterialTheme.colorScheme.primary
                                        isToday     -> MaterialTheme.colorScheme.primaryContainer
                                        allDone     -> MaterialTheme.colorScheme.tertiaryContainer
                                        isVacation  -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                        holiday != null -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                                        else        -> androidx.compose.ui.graphics.Color.Transparent
                                    }
                                )
                                .combinedClickable(
                                    onClick      = { onDayClick(date) },
                                    onLongClick  = { onDayLongPress(date) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text  = "$dayNum",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = when {
                                        isSelected  -> MaterialTheme.colorScheme.onPrimary
                                        isToday     -> MaterialTheme.colorScheme.primary
                                        holiday != null && !isWeekend -> MaterialTheme.colorScheme.error
                                        isWeekend   -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else        -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                // Nombre de ruta en miniatura (solo si 1 ruta)
                                if (hasRoutes && routes.size == 1) {
                                    Text(
                                        text     = routes[0].name,
                                        style    = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = androidx.compose.ui.unit.TextUnit(
                                                7f, androidx.compose.ui.unit.TextUnitType.Sp)
                                        ),
                                        color    = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            allDone    -> MaterialTheme.colorScheme.secondary
                                            else       -> MaterialTheme.colorScheme.tertiary
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 2.dp),
                                    )
                                }
                                when {
                                    hasRoutes -> {
                                        val routeCount = routes.size
                                        if (routeCount > 1) {
                                            // Mostrar número de rutas si hay más de una
                                            Text(
                                                text  = "$routeCount",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = androidx.compose.ui.unit.TextUnit(7f, androidx.compose.ui.unit.TextUnitType.Sp)
                                                ),
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    allDone    -> MaterialTheme.colorScheme.secondary
                                                    else       -> MaterialTheme.colorScheme.tertiary
                                                },
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            )
                                        } else {
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
                                    isVacation && !isSelected && !hasRoutes -> Box(
                                        modifier = Modifier.size(4.dp).clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f))
                                    )
                                    holiday != null && !isSelected -> Box(
                                        modifier = Modifier.size(4.dp).clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
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
private fun CalendarioRouteCard(
    route:           RouteEntity,
    onClick:         () -> Unit,
    onRemoveFromDay: (() -> Unit)? = null,
) {
    val st = RouteStatusTokens.of(route.status)
    val statusColor = st.color
    val statusIcon  = st.icon
    val statusLabel = st.label

    // Parsear todas las fechas programadas para mostrarlas
    val allDates = remember(route.scheduledDates, route.dateAssigned) {
        val dates = (route.scheduledDates ?: emptyList()).toMutableList()
        if (route.dateAssigned.isNotBlank() && route.dateAssigned != "1970-01-01"
            && !dates.contains(route.dateAssigned)) {
            dates.add(0, route.dateAssigned)
        }
        dates.sorted()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick  = onClick,
    ) {
        Row(
            modifier          = Modifier.padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.md, bottom = Spacing.md),
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
                if (allDates.size > 1) {
                    // Mostrar chips de fechas programadas
                    val fmt = java.time.format.DateTimeFormatter.ofPattern("d/M")
                    val dateChips = allDates.mapNotNull { d ->
                        runCatching { java.time.LocalDate.parse(d).format(fmt) }.getOrNull()
                    }.joinToString(" · ")
                    Text(
                        "📅 $dateChips",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
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
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
            }
            // Botón quitar solo de este día
            if (onRemoveFromDay != null) {
                IconButton(onClick = onRemoveFromDay, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.RemoveCircleOutline,
                        contentDescription = "Quitar de este día",
                        tint   = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

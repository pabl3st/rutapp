package com.pabl3st.rutapp.feature.calendario

import com.pabl3st.rutapp.core.UserRole

import org.json.JSONArray
import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.session.SessionManager
import com.pabl3st.rutapp.data.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class PublicHoliday(
    val date:        String,   // ISO "YYYY-MM-DD"
    val localName:   String,   // nombre en español
    val name:        String,   // nombre en inglés (fallback)
)

data class CalendarioUiState(
    val currentMonth:   YearMonth                      = YearMonth.now(),
    val today:          LocalDate                      = LocalDate.now(),
    val selectedDay:    LocalDate?                     = LocalDate.now(),
    val routesByDate:   Map<String, List<RouteEntity>> = emptyMap(),
    val selectedRoutes: List<RouteEntity>              = emptyList(),
    val holidays:       Map<String, PublicHoliday>     = emptyMap(),
    val isLoading:      Boolean                        = true,
    // Long press context menu
    val showDayMenu:    Boolean                        = false,
    val menuDay:        LocalDate?                     = null,
    // Selector de rutas para asignar a un día
    val showRouteSelector: Boolean                     = false,
    val allRoutes:         List<RouteEntity>           = emptyList(),
    val snackbar:          String?                     = null,
    val error:             String?                     = null,
    val vacationDays:      Set<String>                 = emptySet(),
    val isManager:         Boolean                     = false,
)

@HiltViewModel
class CalendarioViewModel @Inject constructor(
    private val routeRepo:     RouteRepository,
    private val userPrefsRepo: UserPrefsRepository,
    private val session:       SessionManager,
) : BaseViewModel() {

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val _ui = MutableStateFlow(CalendarioUiState(
        isManager = UserRole.from(session.userRole).canViewTeam,
    ))
    val ui: StateFlow<CalendarioUiState> = _ui.asStateFlow()

    // Cache de festivos por año para no repetir llamadas
    private val holidayCache = mutableMapOf<Int, Map<String, PublicHoliday>>()

    init {
        observeAllRoutes()
        observeVacations()
        fetchHolidaysForYear(LocalDate.now().year)
    }

    private fun observeVacations() {
        viewModelScope.launch {
            userPrefsRepo.prefs.collect { p ->
                _ui.update { it.copy(vacationDays = p.vacationDays) }
            }
        }
    }

    private fun observeAllRoutes() {
        viewModelScope.launch {
            routeRepo.observeAll()
                .catch { }
                .collect { routes ->
                    val active = routes.filter { it.deletedAt == null }
                    // Una ruta aparece en el calendario en su dateAssigned
                    // Y también en todas sus scheduledDates (fechas recurrentes)
                    val byDate = mutableMapOf<String, MutableList<com.pabl3st.rutapp.data.local.entity.RouteEntity>>()
                    active.forEach { route ->
                        // Fecha principal
                        byDate.getOrPut(route.dateAssigned) { mutableListOf() }.add(route)
                        // Fechas adicionales programadas
                        route.scheduledDates?.forEach { d ->
                            if (d.isNotEmpty() && d != route.dateAssigned) {
                                byDate.getOrPut(d) { mutableListOf() }.add(route)
                            }
                        }
                    }
                    val selected = _ui.value.selectedDay?.format(fmt)
                    _ui.update {
                        it.copy(
                            routesByDate   = byDate,
                            selectedRoutes = if (selected != null) byDate[selected] ?: emptyList() else emptyList(),
                            allRoutes      = active,
                            isLoading      = false,
                        )
                    }
                }
        }
    }

    fun selectDay(day: LocalDate) {
        val dateStr = day.format(fmt)
        _ui.update {
            it.copy(
                selectedDay    = day,
                selectedRoutes = it.routesByDate[dateStr] ?: emptyList(),
            )
        }
    }

    fun prevMonth() {
        val newMonth = _ui.value.currentMonth.minusMonths(1)
        _ui.update { it.copy(currentMonth = newMonth) }
        fetchHolidaysForYear(newMonth.year)
    }

    fun nextMonth() {
        val newMonth = _ui.value.currentMonth.plusMonths(1)
        _ui.update { it.copy(currentMonth = newMonth) }
        fetchHolidaysForYear(newMonth.year)
    }

    /** Vuelve al mes actual y selecciona el día de hoy */
    fun goToToday() {
        val now = java.time.YearMonth.now()
        _ui.update { it.copy(currentMonth = now, selectedDay = java.time.LocalDate.now()) }
        fetchHolidaysForYear(now.year)
    }

    private fun fetchHolidaysForYear(year: Int) {
        if (holidayCache.containsKey(year)) {
            _ui.update { it.copy(holidays = mergeHolidays(it.holidays, holidayCache[year]!!)) }
            return
        }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url  = "https://date.nager.at/api/v3/PublicHolidays/$year/ES"
                    val json = URL(url).readText(Charsets.UTF_8)
                    parseHolidays(json)
                }
                holidayCache[year] = result
                _ui.update { it.copy(holidays = mergeHolidays(it.holidays, result)) }
            } catch (_: Exception) {
                // Sin red o fallo de API — calendario sigue funcionando sin festivos
            }
        }
    }

    private fun parseHolidays(json: String): Map<String, PublicHoliday> {
        val arr  = JSONArray(json)
        val map  = mutableMapOf<String, PublicHoliday>()
        for (i in 0 until arr.length()) {
            val obj  = arr.getJSONObject(i)
            val date = obj.getString("date")
            map[date] = PublicHoliday(
                date      = date,
                localName = obj.optString("localName", ""),
                name      = obj.optString("name", ""),
            )
        }
        return map
    }

    private fun mergeHolidays(
        existing: Map<String, PublicHoliday>,
        new:      Map<String, PublicHoliday>,
    ): Map<String, PublicHoliday> = existing + new

    // ── Long press: menú contextual del día ───────────────
    fun onDayLongPress(day: LocalDate) {
        _ui.update { it.copy(showDayMenu = true, menuDay = day, selectedDay = day) }
    }

    fun dismissDayMenu() {
        _ui.update { it.copy(showDayMenu = false, menuDay = null) }
    }

    // ── Marcar vacaciones ──────────────────────────────────
    fun onMarkVacation() {
        val day = _ui.value.menuDay ?: return
        val dateStr = day.format(fmt)
        viewModelScope.launch {
            userPrefsRepo.toggleVacationDay(dateStr)
            val isNowVacation = userPrefsRepo.isVacationDay(
                userPrefsRepo.prefs.first(), dateStr
            )
            val msg = if (isNowVacation)
                "Día ${day.dayOfMonth}/${day.monthValue} marcado como vacaciones"
            else
                "Día ${day.dayOfMonth}/${day.monthValue} quitado de vacaciones"
            _ui.update { it.copy(showDayMenu = false, menuDay = null, snackbar = msg) }
        }
    }

    // ── Quitar ruta del día concreto (no de todos los días) ──
    fun onRemoveRoute() {
        val day     = _ui.value.menuDay ?: return
        val dateStr = day.format(fmt)
        val routes  = _ui.value.routesByDate[dateStr] ?: return
        viewModelScope.launch {
            routes.forEach { route ->
                // Quita solo esta fecha del array — las demás fechas de la ruta se conservan
                routeRepo.unassignDate(route.uid, dateStr)
            }
            val names = routes.joinToString(", ") { it.name }
            _ui.update { it.copy(snackbar = "\"$names\" quitada del ${day.dayOfMonth}/${day.monthValue}") }
        }
        dismissDayMenu()
    }

    // ── Quitar UNA ruta concreta del día ──────────────────
    fun onRemoveRouteFromDay(route: RouteEntity) {
        val day     = _ui.value.menuDay ?: _ui.value.selectedDay ?: return
        val dateStr = day.format(fmt)
        viewModelScope.launch {
            routeRepo.unassignDate(route.uid, dateStr)
            _ui.update { it.copy(snackbar = "\"${route.name}\" quitada del ${day.dayOfMonth}/${day.monthValue}") }
        }
    }

    // ── Mostrar selector de ruta para asignar ─────────────
    fun onShowRouteSelector() {
        _ui.update { it.copy(showDayMenu = false, showRouteSelector = true) }
    }

    fun onDismissRouteSelector() {
        _ui.update { it.copy(showRouteSelector = false, menuDay = null) }
    }

    /** Asigna la ruta seleccionada al día del menú contextual */
    fun onAssignRouteToDay(route: RouteEntity) {
        val day = _ui.value.menuDay ?: return
        val dateStr = day.format(fmt)
        viewModelScope.launch {
            routeRepo.assignDate(route.uid, dateStr)
            _ui.update { it.copy(
                showRouteSelector = false,
                menuDay           = null,
                snackbar          = "\"${route.name}\" asignada al ${day.dayOfMonth}/${day.monthValue}",
            )}
        }
    }

    fun clearSnackbar() = _ui.update { it.copy(snackbar = null) }
    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(error = t.message ?: "Error inesperado") }
    }

}

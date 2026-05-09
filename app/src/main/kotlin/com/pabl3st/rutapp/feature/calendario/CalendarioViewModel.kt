package com.pabl3st.rutapp.feature.calendario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
)

@HiltViewModel
class CalendarioViewModel @Inject constructor(
    private val routeRepo: RouteRepository,
) : ViewModel() {

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val _ui = MutableStateFlow(CalendarioUiState())
    val ui: StateFlow<CalendarioUiState> = _ui.asStateFlow()

    // Cache de festivos por año para no repetir llamadas
    private val holidayCache = mutableMapOf<Int, Map<String, PublicHoliday>>()

    init {
        observeAllRoutes()
        fetchHolidaysForYear(LocalDate.now().year)
    }

    private fun observeAllRoutes() {
        viewModelScope.launch {
            routeRepo.observeAll()
                .catch { }
                .collect { routes ->
                    val byDate = routes
                        .filter { it.deletedAt == null }
                        .groupBy { it.dateAssigned }
                    val selected = _ui.value.selectedDay?.format(fmt)
                    _ui.update {
                        it.copy(
                            routesByDate   = byDate,
                            selectedRoutes = if (selected != null) byDate[selected] ?: emptyList() else emptyList(),
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

    fun onMarkVacation() {
        // TODO: marcar el día como vacaciones en user_prefs
        dismissDayMenu()
    }

    fun onRemoveRoute() {
        val day = _ui.value.menuDay ?: return
        val dateStr = day.format(fmt)
        val routes  = _ui.value.routesByDate[dateStr] ?: return
        // Soft-delete: mover dateAssigned a una fecha vacía equivale a desasignar
        // Por ahora quitar la ruta de la vista local hasta que se implemente API
        viewModelScope.launch {
            routes.forEach { route ->
                routeRepo.unassignDate(route.uid)
            }
        }
        dismissDayMenu()
    }
}

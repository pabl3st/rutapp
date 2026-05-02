package com.pabl3st.rutapp.feature.calendario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CalendarioUiState(
    val currentMonth:  YearMonth              = YearMonth.now(),
    val today:         LocalDate              = LocalDate.now(),
    val selectedDay:   LocalDate?             = LocalDate.now(),
    val routesByDate:  Map<String, List<RouteEntity>> = emptyMap(),
    val selectedRoutes: List<RouteEntity>     = emptyList(),
    val isLoading:     Boolean                = true,
)

@HiltViewModel
class CalendarioViewModel @Inject constructor(
    private val routeRepo: RouteRepository,
) : ViewModel() {

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val _ui = MutableStateFlow(CalendarioUiState())
    val ui: StateFlow<CalendarioUiState> = _ui.asStateFlow()

    init { observeAllRoutes() }

    private fun observeAllRoutes() {
        viewModelScope.launch {
            routeRepo.observeAll()
                .catch { }
                .collect { routes ->
                    val byDate = routes
                        .filter { it.deletedAt == null }
                        .groupBy { it.dateAssigned }
                    val selected = _ui.value.selectedDay?.format(fmt)
                    _ui.update { it.copy(
                        routesByDate   = byDate,
                        selectedRoutes = if (selected != null) byDate[selected] ?: emptyList() else emptyList(),
                        isLoading      = false,
                    )}
                }
        }
    }

    fun selectDay(day: LocalDate) {
        val dateStr = day.format(fmt)
        _ui.update { it.copy(
            selectedDay    = day,
            selectedRoutes = it.routesByDate[dateStr] ?: emptyList(),
        )}
    }

    fun prevMonth() = _ui.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
    fun nextMonth() = _ui.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
}

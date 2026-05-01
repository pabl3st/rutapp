package com.pabl3st.rutapp.feature.rutas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class RutasUiState(
    val routes: List<RouteEntity> = emptyList(),
    val isLoading: Boolean        = true,
    val isSyncing: Boolean        = false,
    val showCreateDialog: Boolean = false,
    val newRouteName: String      = "",
    val newRouteDate: String      = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val error: String?            = null,
)

@HiltViewModel
class RutasViewModel @Inject constructor(
    private val routeRepo: RouteRepository,
    private val session:   SessionManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(RutasUiState())
    val ui: StateFlow<RutasUiState> = _ui.asStateFlow()

    init {
        observeRoutes()
    }

    private fun observeRoutes() {
        viewModelScope.launch {
            routeRepo.observeAll()
                .catch { e -> _ui.update { it.copy(error = e.message, isLoading = false) } }
                .collect { routes -> _ui.update { it.copy(routes = routes, isLoading = false) } }
        }
    }

    // ── Crear ruta ────────────────────────────────────────────
    fun onShowCreateDialog()            = _ui.update { it.copy(showCreateDialog = true) }
    fun onDismissCreateDialog()         = _ui.update { it.copy(showCreateDialog = false, newRouteName = "", error = null) }
    fun onNewRouteNameChange(v: String) = _ui.update { it.copy(newRouteName = v) }
    fun onNewRouteDateChange(v: String) = _ui.update { it.copy(newRouteDate = v) }

    fun createRoute() {
        val name = _ui.value.newRouteName.trim()
        if (name.isBlank()) {
            _ui.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            routeRepo.createRoute(name = name, dateAssigned = _ui.value.newRouteDate)
            _ui.update { it.copy(showCreateDialog = false, newRouteName = "") }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

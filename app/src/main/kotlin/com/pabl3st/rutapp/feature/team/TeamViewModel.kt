package com.pabl3st.rutapp.feature.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.BaseViewModel
import com.pabl3st.rutapp.data.network.AgentOverviewDto
import com.pabl3st.rutapp.data.repository.TeamRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamUiState(
    val agents:       List<AgentOverviewDto> = emptyList(),
    val isLoading:    Boolean                = true,
    val isRefreshing: Boolean                = false,
    val error:        String?                = null,
    val userRole:     String                 = "agent",
    val filterActive: Boolean                = false,
    /** Si no es null, esta pantalla es un drill-down: "Equipo de XXX" */
    val viewAsUserId:   Int?    = null,
    val viewAsUserName: String? = null,
)

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepo: TeamRepository,
    private val session:  SessionManager,
    savedStateHandle:     SavedStateHandle,
) : BaseViewModel() {

    /** Drill-down parameters — null para ver el equipo del propio usuario logueado.
     *  Si se pasan, la pantalla muestra "Equipo de XXX" y consulta el server
     *  con for_user_id. El server verifica que el target está en el subárbol
     *  del caller (403 si no).
     *  El viewAsUserId llega como String? desde la URL para soportar valor
     *  ausente (NavType.IntType no soporta null nativamente). */
    private val viewAsUserId: Int? = savedStateHandle.get<String>("viewAsUserId")?.toIntOrNull()
    private val viewAsUserName: String? = savedStateHandle.get<String>("viewAsUserName")

    private val _ui = MutableStateFlow(TeamUiState(
        userRole       = session.userRole,
        viewAsUserId   = viewAsUserId,
        viewAsUserName = viewAsUserName,
    ))
    val ui: StateFlow<TeamUiState> = _ui.asStateFlow()

    init { load(); startPolling() }

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            teamRepo.teamOverview(forUserId = viewAsUserId)
                .onSuccess { agents -> _ui.update { it.copy(agents = agents, isLoading = false) } }
                .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isRefreshing = true) }
            teamRepo.teamOverview(forUserId = viewAsUserId)
                .onSuccess { agents -> _ui.update { it.copy(agents = agents, isRefreshing = false) } }
                .onFailure { _ui.update { it.copy(isRefreshing = false) } }
        }
    }

    fun toggleActiveFilter() = _ui.update { it.copy(filterActive = !it.filterActive) }
    fun clearError()         = _ui.update { it.copy(error = null) }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                if (!_ui.value.isLoading) {
                    teamRepo.teamOverview(forUserId = viewAsUserId).onSuccess { agents ->
                        _ui.update { it.copy(agents = agents) }
                    }
                }
            }
        }
    }

    val filteredAgents: StateFlow<List<AgentOverviewDto>> = _ui
        .map { s -> if (s.filterActive) s.agents.filter { it.isActive || it.isPaused } else s.agents }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

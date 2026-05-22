package com.pabl3st.rutapp.feature.team

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
)

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepo: TeamRepository,
    private val session:  SessionManager,
) : BaseViewModel() {

    private val _ui = MutableStateFlow(TeamUiState(userRole = session.userRole))
    val ui: StateFlow<TeamUiState> = _ui.asStateFlow()

    init { load(); startPolling() }

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            teamRepo.teamOverview()
                .onSuccess { agents -> _ui.update { it.copy(agents = agents, isLoading = false) } }
                .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isRefreshing = true) }
            teamRepo.teamOverview()
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
                    teamRepo.teamOverview().onSuccess { agents ->
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

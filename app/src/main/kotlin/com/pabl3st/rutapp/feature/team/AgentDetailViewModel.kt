package com.pabl3st.rutapp.feature.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.BaseViewModel
import com.pabl3st.rutapp.data.network.AgentDetailResponse
import com.pabl3st.rutapp.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentDetailUiState(
    val detail:      AgentDetailResponse? = null,
    val isLoading:   Boolean              = true,
    val isRefreshing:Boolean              = false,
    val error:       String?              = null,
)

@HiltViewModel
class AgentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val teamRepo: TeamRepository,
) : BaseViewModel() {

    private val agentId: Int = checkNotNull(savedStateHandle["agentId"])

    private val _ui = MutableStateFlow(AgentDetailUiState())
    val ui: StateFlow<AgentDetailUiState> = _ui.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            teamRepo.agentDetail(agentId)
                .onSuccess { detail -> _ui.update { it.copy(detail = detail, isLoading = false) } }
                .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isRefreshing = true) }
            teamRepo.agentDetail(agentId)
                .onSuccess { detail -> _ui.update { it.copy(detail = detail, isRefreshing = false) } }
                .onFailure { _ui.update { it.copy(isRefreshing = false) } }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

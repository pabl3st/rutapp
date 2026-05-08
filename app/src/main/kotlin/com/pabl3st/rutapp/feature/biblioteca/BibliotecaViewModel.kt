package com.pabl3st.rutapp.feature.biblioteca

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BibliotecaTab(val label: String, val icon: ImageVector) {
    ALL("Todas", Icons.Default.GridView),
    NO_GPS("Sin GPS", Icons.Default.GpsOff),
    ORPHAN("Sin ruta", Icons.Default.LinkOff),
}

data class BibliotecaUiState(
    val tab: BibliotecaTab         = BibliotecaTab.ALL,
    val query: String              = "",
    val filteredStops: List<StopEntity> = emptyList(),
    val isLoading: Boolean         = true,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class BibliotecaViewModel @Inject constructor(
    private val stopRepo: StopRepository,
    private val session:  SessionManager,
) : ViewModel() {

    private val _tab   = MutableStateFlow(BibliotecaTab.ALL)
    private val _query = MutableStateFlow("")

    private val _ui = MutableStateFlow(BibliotecaUiState())
    val ui: StateFlow<BibliotecaUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_tab, _query.debounce(200)) { tab, query -> tab to query }
                .flatMapLatest { (tab, query) ->
                    val base = when (tab) {
                        BibliotecaTab.ALL    -> stopRepo.observeAll(session.accountId)
                        BibliotecaTab.NO_GPS -> stopRepo.observeWithoutGps(session.accountId)
                        BibliotecaTab.ORPHAN -> stopRepo.observeOrphaned(session.accountId)
                    }
                    base.map { stops ->
                        val filtered = if (query.isBlank()) stops
                        else stops.filter {
                            it.name.contains(query, ignoreCase = true) ||
                            it.address?.contains(query, ignoreCase = true) == true ||
                            it.externalId?.contains(query, ignoreCase = true) == true
                        }
                        Triple(tab, query, filtered)
                    }
                }
                .collect { (tab, query, filtered) ->
                    _ui.update { it.copy(tab = tab, query = query, filteredStops = filtered, isLoading = false) }
                }
        }
    }

    fun onTabChange(tab: BibliotecaTab) { _tab.value = tab }
    fun onQueryChange(q: String)        { _query.value = q }
}


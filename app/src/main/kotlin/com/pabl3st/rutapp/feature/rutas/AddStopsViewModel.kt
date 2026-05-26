package com.pabl3st.rutapp.feature.rutas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.BaseViewModel
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AddStopsSortMode { NAME, POSTAL_CODE, LOCALITY, PROXIMITY }

data class AddStopsUiState(
    val stops:         List<StopEntity> = emptyList(),
    val selected:      Set<String>      = emptySet(),
    val query:         String           = "",
    val sortMode:      AddStopsSortMode = AddStopsSortMode.NAME,
    val groupByProxim: Boolean          = false,
    val isLoading:     Boolean          = true,
    val isSaving:      Boolean          = false,
    val done:          Boolean          = false,
    val error:         String?          = null,
)

fun extractPostalCode(address: String?): String =
    address?.let { Regex("""\b\d{5}\b""").find(it)?.value } ?: ""

fun extractLocality(address: String?): String {
    if (address == null) return ""
    val m = Regex("""\b\d{5}\b\s*(.+)""").find(address)
    if (m != null) return m.groupValues[1].trim()
    return address.substringAfterLast(",").trim()
}

/** Filtro y orden de la lista — calculado de forma pura fuera del VM */
private fun applyFilter(
    stops:     List<StopEntity>,
    inRoute:   Set<String>,
    query:     String,
    sortMode:  AddStopsSortMode,
    proxim:    Boolean,
): List<StopEntity> {
    // Excluir los ya en la ruta
    val available = stops.filter { it.uid !in inRoute }

    // Deduplicar por externalId
    val deduped: List<StopEntity> = available
        .groupBy { it.externalId ?: it.uid }
        .mapValues { (_, g) ->
            g.maxByOrNull { it.visitedAt ?: "" } ?: g.first()
        }
        .values
        .toList()

    // Filtrar por query
    val filtered = if (query.isBlank()) deduped else deduped.filter { s ->
        s.name.contains(query, ignoreCase = true) ||
        s.externalId?.contains(query, ignoreCase = true) == true ||
        s.address?.contains(query, ignoreCase = true) == true ||
        extractPostalCode(s.address).startsWith(query) ||
        extractLocality(s.address).contains(query, ignoreCase = true)
    }

    // Ordenar
    return when {
        proxim || sortMode == AddStopsSortMode.PROXIMITY ->
            filtered.sortedWith(compareBy({ extractPostalCode(it.address) }, { it.name.lowercase() }))
        sortMode == AddStopsSortMode.POSTAL_CODE ->
            filtered.sortedBy { extractPostalCode(it.address) }
        sortMode == AddStopsSortMode.LOCALITY ->
            filtered.sortedBy { extractLocality(it.address).lowercase() }
        else -> // NAME
            filtered.sortedBy { it.name.lowercase() }
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class AddStopsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stopRepo: StopRepository,
    private val session:  SessionManager,
) : BaseViewModel() {

    private val routeUid: String = checkNotNull(savedStateHandle["routeUid"])

    private val _query    = MutableStateFlow("")
    private val _sort     = MutableStateFlow(AddStopsSortMode.NAME)
    private val _proxim   = MutableStateFlow(false)
    private val _selected = MutableStateFlow<Set<String>>(emptySet())

    private val _ui = MutableStateFlow(AddStopsUiState())
    val ui: StateFlow<AddStopsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            // Combinar de a 5 máximo — Kotlin solo tiene combine hasta 5 overloads
            // Paso 1: combinar los 2 flows de datos
            val dataFlow: Flow<Pair<List<StopEntity>, Set<String>>> = combine(
                stopRepo.observeAll(session.accountId),
                stopRepo.observeByRoute(routeUid).map { list -> list.map { it.uid }.toSet() },
            ) { all, inRoute -> all to inRoute }

            // Paso 2: combinar con los 3 flows de parámetros
            combine(
                dataFlow,
                _query.debounce(150),
                _sort,
                _proxim,
                _selected,
            ) { (all, inRoute), query, sort, proxim, selected ->
                val stops = applyFilter(all, inRoute, query, sort, proxim)
                AddStopsUiState(
                    stops         = stops,
                    selected      = selected,
                    query         = query,
                    sortMode      = sort,
                    groupByProxim = proxim,
                    isLoading     = false,
                )
            }.collect { state -> _ui.value = state }
        }
    }

    fun onQueryChange(q: String)              { _query.value = q }
    fun onSortChange(m: AddStopsSortMode)     { _sort.value = m; _proxim.value = false }
    fun onToggleProximity()                   { _proxim.value = !_proxim.value }

    fun onToggleSelect(uid: String) {
        _selected.update { if (uid in it) it - uid else it + uid }
    }
    fun onSelectAll() {
        _selected.update { _ui.value.stops.map { it.uid }.toSet() }
    }
    fun onClearSelection() { _selected.update { emptySet() } }

    fun confirmAdd() {
        val toAdd = _ui.value.selected.toList()
        if (toAdd.isEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true) }
            val date = java.time.LocalDate.now().toString()
            toAdd.forEach { uid -> stopRepo.addToRoute(uid, routeUid, date) }
            _ui.update { it.copy(isSaving = false, done = true) }
        }
    }

    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(isLoading = false, error = t.message ?: "Error inesperado") }
    }
}

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
    val stops:          List<StopEntity> = emptyList(),  // filtrados y ordenados
    val selected:       Set<String>      = emptySet(),   // UIDs seleccionados
    val query:          String           = "",
    val sortMode:       AddStopsSortMode = AddStopsSortMode.NAME,
    val groupByProxim:  Boolean          = false,
    val isLoading:      Boolean          = true,
    val isSaving:       Boolean          = false,
    val done:           Boolean          = false,
    val error:          String?          = null,
)

/** Extrae CP de un string de dirección tipo "Calle X 12, 46001 Valencia" */
fun extractPostalCode(address: String?): String =
    address?.let { Regex("""\b\d{5}\b""").find(it)?.value } ?: ""

/** Extrae localidad (texto después del CP si existe, o último segmento tras coma) */
fun extractLocality(address: String?): String {
    if (address == null) return ""
    val m = Regex("""\b\d{5}\b\s*(.+)""").find(address)
    if (m != null) return m.groupValues[1].trim()
    return address.substringAfterLast(",").trim()
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

    // Todos los stops de la cuenta (biblioteca completa)
    private val allStops: StateFlow<List<StopEntity>> =
        stopRepo.observeAll(session.accountId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Stops ya en la ruta (para excluirlos)
    private val routeStopUids: StateFlow<Set<String>> =
        stopRepo.observeByRoute(routeUid)
            .map { list -> list.map { it.uid }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            combine(
                allStops, routeStopUids, _query.debounce(150), _sort, _proxim, _selected
            ) { all, inRoute, q, sort, proxim, sel ->
                // Excluir los que ya están en la ruta
                val available = all.filter { it.uid !in inRoute }
                // Deduplicar por externalId
                val deduped = available
                    .groupBy { it.externalId ?: it.uid }
                    .mapValues { (_, g) -> g.maxByOrNull { it.visitedAt ?: "" } ?: g.first() }
                    .values.toList()
                // Filtrar por query (nombre, id externo, dirección)
                val filtered = if (q.isBlank()) deduped else deduped.filter {
                    it.name.contains(q, ignoreCase = true) ||
                    it.externalId?.contains(q, ignoreCase = true) == true ||
                    it.address?.contains(q, ignoreCase = true) == true ||
                    extractPostalCode(it.address).startsWith(q) ||
                    extractLocality(it.address).contains(q, ignoreCase = true)
                }
                // Ordenar
                val sorted = when {
                    proxim -> groupByProximity(filtered)
                    else   -> when (sort) {
                        AddStopsSortMode.NAME        -> filtered.sortedBy { it.name.lowercase() }
                        AddStopsSortMode.POSTAL_CODE -> filtered.sortedBy { extractPostalCode(it.address) }
                        AddStopsSortMode.LOCALITY    -> filtered.sortedBy { extractLocality(it.address).lowercase() }
                        AddStopsSortMode.PROXIMITY   -> groupByProximity(filtered)
                    }
                }
                AddStopsUiState(stops = sorted, selected = sel,
                    query = q, sortMode = sort, groupByProxim = proxim,
                    isLoading = false)
            }.collect { state -> _ui.value = state }
        }
    }

    /** Agrupar por proximidad geográfica (cluster por CP primero, luego por coords) */
    private fun groupByProximity(stops: List<StopEntity>): List<StopEntity> {
        val withCp    = stops.sortedWith(compareBy({ extractPostalCode(it.address) }, { it.name }))
        val withoutCp = stops.filter { extractPostalCode(it.address).isBlank() }.sortedBy { it.name }
        return withCp + withoutCp
    }

    fun onQueryChange(q: String)    { _query.value = q }
    fun onSortChange(m: AddStopsSortMode) { _sort.value = m; _proxim.value = false }
    fun onToggleProximity()         { _proxim.value = !_proxim.value }

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

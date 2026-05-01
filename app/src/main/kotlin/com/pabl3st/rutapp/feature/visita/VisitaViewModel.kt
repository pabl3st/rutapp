package com.pabl3st.rutapp.feature.visita

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VisitaUiState(
    val stop: StopEntity?       = null,
    val isLoading: Boolean      = true,
    val isSaving: Boolean       = false,
    val selectedResult: String  = "contactado",   // contactado|no_estaba|volvemos|rechazado
    val notes: String           = "",
    val nextAction: String      = "",
    val saved: Boolean          = false,
    val error: String?          = null,
)

@HiltViewModel
class VisitaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stopRepo: StopRepository,
) : ViewModel() {

    private val stopUid: String = checkNotNull(savedStateHandle["stopUid"])

    private val _ui = MutableStateFlow(VisitaUiState())
    val ui: StateFlow<VisitaUiState> = _ui.asStateFlow()

    init { loadStop() }

    private fun loadStop() {
        viewModelScope.launch {
            stopRepo.markVisiting(stopUid)  // marca el stop como en visita al abrir el formulario
            val stop = stopRepo.getByUid(stopUid)
            _ui.update {
                it.copy(
                    stop           = stop,
                    isLoading      = false,
                    selectedResult = stop?.visitResult ?: "contactado",
                    notes          = stop?.notes       ?: "",
                    nextAction     = stop?.nextAction  ?: "",
                )
            }
        }
    }

    fun onResultChange(result: String) = _ui.update { it.copy(selectedResult = result) }
    fun onNotesChange(v: String)       = _ui.update { it.copy(notes = v) }
    fun onNextActionChange(v: String)  = _ui.update { it.copy(nextAction = v) }

    fun saveVisit() {
        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true) }
            stopRepo.saveVisitResult(
                uid        = stopUid,
                result     = _ui.value.selectedResult,
                notes      = _ui.value.notes.trim().ifEmpty { null },
                nextAction = _ui.value.nextAction.trim().ifEmpty { null },
            )
            _ui.update { it.copy(isSaving = false, saved = true) }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

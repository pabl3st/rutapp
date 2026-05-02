package com.pabl3st.rutapp.feature.visita

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VisitaUiState(
    val stop: StopEntity?       = null,
    val isLoading: Boolean      = true,
    val isSaving: Boolean       = false,
    val selectedResult: String  = "contactado",
    val notes: String           = "",
    val nextAction: String      = "",
    val photos: List<Uri>       = emptyList(),
    val showCamera: Boolean     = false,
    val saved: Boolean          = false,
    val kpiFields: List<KpiDefinitionEntity> = emptyList(), // KPIs visibles del sector activo
    val kpiValues: Map<String, String>          = emptyMap(),  // kpiId -> valor introducido
    val error: String?          = null,
)

@HiltViewModel
class VisitaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stopRepo:    StopRepository,
    private val profileRepo: BusinessProfileRepository,
    private val kpiValueDao: KpiValueDao,
) : ViewModel() {

    private val stopUid: String = checkNotNull(savedStateHandle["stopUid"])

    private val _ui = MutableStateFlow(VisitaUiState())
    val ui: StateFlow<VisitaUiState> = _ui.asStateFlow()

    init {
        loadStop()
        loadKpiFields()
        loadExistingKpiValues()
    }

    private fun loadStop() {
        viewModelScope.launch {
            stopRepo.markVisiting(stopUid)
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

    fun onResultChange(result: String)  = _ui.update { it.copy(selectedResult = result) }
    fun onNotesChange(v: String)        = _ui.update { it.copy(notes = v) }
    fun onNextActionChange(v: String)   = _ui.update { it.copy(nextAction = v) }

    fun onShowCamera()                  = _ui.update { it.copy(showCamera = true) }
    fun onHideCamera()                  = _ui.update { it.copy(showCamera = false) }

    fun onPhotoTaken(uri: Uri) {
        _ui.update { it.copy(photos = it.photos + uri, showCamera = false) }
    }

    fun onRemovePhoto(uri: Uri) {
        _ui.update { it.copy(photos = it.photos - uri) }
    }

    fun saveVisit() {
        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true) }

            // 1. Guardar resultado de visita en Stop
            stopRepo.saveVisitResult(
                uid        = stopUid,
                result     = _ui.value.selectedResult,
                notes      = _ui.value.notes.trim().ifEmpty { null },
                nextAction = _ui.value.nextAction.trim().ifEmpty { null },
            )

            // 2. Persistir valores KPI en Room + marcar pendiente de sync
            val kpiEntities = _ui.value.kpiValues
                .filter { (_, v) -> v.isNotBlank() }
                .map { (kpiId, value) ->
                    KpiValueEntity(
                        stopUid    = stopUid,
                        kpiId      = kpiId,
                        valueText  = value.trim(),
                        syncStatus = "pending",
                    )
                }
            if (kpiEntities.isNotEmpty()) {
                kpiValueDao.upsertAll(kpiEntities)
                // Encolar sync como una operación de tipo kpi_values
                stopRepo.enqueueKpiValuesSync(stopUid, _ui.value.kpiValues)
            }

            _ui.update { it.copy(isSaving = false, saved = true) }
        }
    }

    private fun loadExistingKpiValues() {
        viewModelScope.launch {
            val existing = kpiValueDao.getByStop(stopUid)
                .associate { it.kpiId to it.valueText }
            _ui.update { it.copy(kpiValues = it.kpiValues + existing) }
        }
    }

    private fun loadKpiFields() {
        viewModelScope.launch {
            val profile  = profileRepo.getOrCreateProfile()
            val kpis     = profileRepo.getVisibleKpisForSector(profile.sector)
            // Excluir los ya cubiertos por campos fijos (resultado, notas, próxima acción)
            val filtered = kpis.filter { it.id !in setOf(
                "common_resultado", "common_notas", "common_next_action"
            )}
            _ui.update { it.copy(kpiFields = filtered) }
        }
    }

    fun onKpiValueChange(kpiId: String, value: String) {
        _ui.update { it.copy(kpiValues = it.kpiValues + (kpiId to value)) }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

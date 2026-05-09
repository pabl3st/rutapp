package com.pabl3st.rutapp.feature.visita

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository
import com.pabl3st.rutapp.data.repository.UserPrefs
import com.pabl3st.rutapp.data.repository.UserPrefsRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VisitaUiState(
    val stop: StopEntity?                    = null,
    val isLoading: Boolean                   = true,
    val isSaving: Boolean                    = false,
    // "" = sin selección (formulario nuevo); valor = edición de visita existente
    val selectedResult: String               = "",
    val notes: String                        = "",
    val nextAction: String                   = "",
    // null = sin selección; true/false = estado PDV seleccionado
    val storeOpen: Boolean?                  = null,
    val isEditingPreviousVisit: Boolean      = false, // true si el stop ya era 'done'
    val photos: List<Uri>                    = emptyList(),
    val showCamera: Boolean                  = false,
    val saved: Boolean                       = false,
    val prefs: UserPrefs                     = UserPrefs(),
    val kpiFields: List<KpiDefinitionEntity> = emptyList(),
    val kpiValues: Map<String, String>       = emptyMap(),
    val error: String?                       = null,
)

@HiltViewModel
class VisitaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stopRepo:     StopRepository,
    private val profileRepo:  BusinessProfileRepository,
    private val kpiValueDao:  KpiValueDao,
    private val syncQueueDao: SyncQueueDao,
    private val prefsRepo:    UserPrefsRepository,
    private val moshi:        Moshi,
) : ViewModel() {

    private val stopUid: String = checkNotNull(savedStateHandle["stopUid"])

    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    private val _ui = MutableStateFlow(VisitaUiState())
    val ui: StateFlow<VisitaUiState> = _ui.asStateFlow()

    init {
        loadStop()
        loadKpiFields()
        loadPrefs()
        loadExistingKpiValues()
    }

    private fun loadStop() {
        viewModelScope.launch {
            val stop = stopRepo.getByUid(stopUid) ?: run {
                _ui.update { it.copy(isLoading = false) }
                return@launch
            }

            // ── Reset por visitFrequency ──────────────────────────────────
            // Si el stop está 'done' y han pasado ≥ visitFrequency días desde
            // la última visita → resetear a 'pending' para una nueva visita limpia.
            val shouldReset = stop.status == "done"
                && stop.visitFrequency != null
                && stop.visitedAt != null
                && run {
                    val visitedDate = runCatching {
                        java.time.LocalDate.parse(stop.visitedAt.substring(0, 10))
                    }.getOrNull()
                    val today = java.time.LocalDate.now()
                    visitedDate != null && java.time.temporal.ChronoUnit.DAYS.between(visitedDate, today) >= stop.visitFrequency
                }

            if (shouldReset) {
                stopRepo.resetForNewVisit(stopUid)
                // Tras el reset el stop quede 'pending' — abrir formulario en blanco
                _ui.update {
                    it.copy(
                        stop                 = stop.copy(status = "pending", visitResult = null,
                                                          visitedAt = null, notes = null),
                        isLoading            = false,
                        selectedResult       = "",
                        notes                = "",
                        nextAction           = "",
                        storeOpen            = null,
                        isEditingPreviousVisit = false,
                    )
                }
                stopRepo.markVisiting(stopUid)
                return@launch
            }

            // ── Estado 'done': edición de visita existente ────────────────
            // Pre-cargar datos previos y mostrar banner informativo.
            if (stop.status == "done") {
                _ui.update {
                    it.copy(
                        stop                 = stop,
                        isLoading            = false,
                        selectedResult       = stop.visitResult ?: "",
                        notes                = stop.notes       ?: "",
                        nextAction           = stop.nextAction  ?: "",
                        storeOpen            = stop.pdvOpen,
                        isEditingPreviousVisit = true,
                    )
                }
                return@launch
            }

            // ── Estado 'pending' / 'visiting': formulario nuevo en blanco ─
            stopRepo.markVisiting(stopUid)
            _ui.update {
                it.copy(
                    stop                 = stop,
                    isLoading            = false,
                    selectedResult       = "",
                    notes                = "",
                    nextAction           = "",
                    storeOpen            = null,
                    isEditingPreviousVisit = false,
                )
            }
        }
    }

    fun onResultChange(result: String)   = _ui.update { it.copy(selectedResult = result) }
    fun onNotesChange(v: String)         = _ui.update { it.copy(notes = v) }
    fun onNextActionChange(v: String)    = _ui.update { it.copy(nextAction = v) }
    fun onStoreOpenChange(v: Boolean?)   = _ui.update { it.copy(storeOpen = v) }

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

            // 1. Guardar resultado de visita en Stop (encola el stop en SyncQueue via StopRepository)
            stopRepo.saveVisitResult(
                uid        = stopUid,
                result     = _ui.value.selectedResult,
                notes      = _ui.value.notes.trim().ifEmpty { null },
                nextAction = _ui.value.nextAction.trim().ifEmpty { null },
                pdvOpen    = _ui.value.storeOpen ?: true,
            )

            // 2. Persistir valores KPI en Room
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

                // 3. Encolar en SyncQueue para que SyncWorker los suba al servidor
                // api.php batch_sync entity="kpi_values": data={stopUid, values:{kpiId->value}}
                val valuesMap: Map<String, Any?> = kpiEntities.associate { it.kpiId to it.valueText }
                val payload = mapAdapter.toJson(
                    mapOf("stopUid" to stopUid, "values" to valuesMap)
                )
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entity    = "kpi_values",
                        entityUid = stopUid,
                        operation = "upsert",
                        payload   = payload,
                    )
                )
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

    private fun loadPrefs() {
        viewModelScope.launch {
            prefsRepo.prefs.collect { p -> _ui.update { it.copy(prefs = p) } }
        }
    }

    private fun loadKpiFields() {
        viewModelScope.launch {
            val profile  = profileRepo.getOrCreateProfile()
            val kpis     = profileRepo.getVisibleKpisForSector(profile.sector)
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



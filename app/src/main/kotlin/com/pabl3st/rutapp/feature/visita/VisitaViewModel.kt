package com.pabl3st.rutapp.feature.visita

import com.pabl3st.rutapp.core.BaseViewModel
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
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.session.SessionManager
import com.pabl3st.rutapp.data.repository.PhotoRepository
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
    val selectedResult: String               = "contactado",
    val notes: String                        = "",
    val nextAction: String                   = "",
    val storeOpen: Boolean                   = true,
    val pdvInactive: Boolean                 = false,  // PDV cerrado definitivamente
    val isEditingPreviousVisit: Boolean       = false,  // siempre false — formulario siempre limpio
    val photos: List<Uri>                    = emptyList(),
    val showCamera: Boolean                  = false,
    val saved: Boolean                       = false,
    val prefs: UserPrefs                     = UserPrefs(),
    val kpiFields: List<KpiDefinitionEntity> = emptyList(),
    val kpiValues: Map<String, String>       = emptyMap(),
    val error: String?                       = null,
    val previousVisits: List<StopEntity>     = emptyList(),
)

@HiltViewModel
class VisitaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stopRepo:     StopRepository,
    private val routeRepo:    RouteRepository,
    private val session:      SessionManager,
    private val profileRepo:  BusinessProfileRepository,
    private val photoRepo:    PhotoRepository,
    private val kpiValueDao:  KpiValueDao,
    private val syncQueueDao: SyncQueueDao,
    private val prefsRepo:    UserPrefsRepository,
    private val moshi:        Moshi,
    private val locationMgr:  com.pabl3st.rutapp.core.location.LocationManager,
) : BaseViewModel() {

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
        observeExistingPhotos()
    }

    private fun loadStop() {
        viewModelScope.launch {
            val stop = stopRepo.getByUid(stopUid)
            if (stop != null) {
                // Verificar acceso según rol
                val route    = routeRepo.getByUid(stop.routeUid)
                val myRole   = session.userRole
                val myId     = session.userId
                val hasAccess = when (myRole) {
                    "owner", "admin", "god" -> true
                    "manager" -> route?.userId == myId ||
                        (route != null && route.userId in session.managedAgentIds)
                    else -> route?.userId == myId
                }
                if (!hasAccess) {
                    _ui.update { it.copy(isLoading = false, error = "Sin acceso a esta parada") }
                    return@launch
                }
            }
            val history = if (stop != null) stopRepo.getVisitHistory(stopUid) else emptyList()
            _ui.update {
                it.copy(
                    stop           = stop,
                    isLoading      = false,
                    previousVisits = history,
                    selectedResult = "contactado", // siempre limpio al abrir
                    notes          = "",
                    nextAction     = "",
                    storeOpen      = true,         // por defecto abierto
                    pdvInactive    = stop?.pdvInactive ?: false,
                )
            }
        }
    }

    fun onResultChange(result: String)  = _ui.update { it.copy(selectedResult = result) }
    fun onNotesChange(v: String)        = _ui.update { it.copy(notes = v) }
    fun onNextActionChange(v: String)   = _ui.update { it.copy(nextAction = v) }
    fun onStoreOpenChange(v: Boolean)   = _ui.update { it.copy(storeOpen = v) }
    fun onPdvInactiveToggle()            = _ui.update { it.copy(pdvInactive = !it.pdvInactive) }

    fun onShowCamera()                  = _ui.update { it.copy(showCamera = true) }
    fun onHideCamera()                  = _ui.update { it.copy(showCamera = false) }

    fun onPhotoTaken(uri: Uri) {
        _ui.update { it.copy(photos = it.photos + uri, showCamera = false) }
    }

    fun onRemovePhoto(uri: Uri) {
        _ui.update { it.copy(photos = it.photos - uri) }
    }

    fun saveVisit() {
        // Guardia: no guardar si no hay resultado seleccionado
        if (_ui.value.selectedResult.isBlank()) {
            _ui.update { it.copy(error = "Selecciona un resultado antes de guardar") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true) }
            val gpsPos = locationMgr.getLastLocation()

            // 1. Guardar resultado de visita en Stop (encola el stop en SyncQueue via StopRepository)
            stopRepo.saveVisitResult(
                uid         = stopUid,
                result      = _ui.value.selectedResult,
                notes       = _ui.value.notes.trim().ifEmpty { null },
                nextAction  = _ui.value.nextAction.trim().ifEmpty { null },
                pdvOpen     = _ui.value.storeOpen,
                pdvInactive = _ui.value.pdvInactive,
                gpsLat      = gpsPos?.latitude,
                gpsLng      = gpsPos?.longitude,
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

            // 4. Persistir fotos en Room (se subirán al servidor en background via SyncWorker)
            val currentPhotos = _ui.value.photos
            if (currentPhotos.isNotEmpty()) {
                photoRepo.savePhotos(stopUid, currentPhotos)
            }

            _ui.update { it.copy(isSaving = false, saved = true) }
        }
    }

    private fun observeExistingPhotos() {
        viewModelScope.launch {
            photoRepo.observeByStop(stopUid).collect { photos ->
                // Mostrar fotos ya guardadas (synced o pending) como URIs
                val existingUris = photos.map { android.net.Uri.parse(it.localPath) }
                // Solo pre-cargar si la lista actual está vacía (no sobreescribir las recién tomadas)
                if (_ui.value.photos.isEmpty()) {
                    _ui.update { it.copy(photos = existingUris) }
                }
            }
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
    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(
            isLoading = false,
            error     = t.message ?: "Error inesperado",
        )}
    }

}

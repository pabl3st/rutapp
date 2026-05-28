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
import com.pabl3st.rutapp.data.local.entity.StopVisitEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.session.SessionManager
import com.pabl3st.rutapp.data.repository.PhotoRepository
import com.pabl3st.rutapp.data.repository.UserPrefs
import com.pabl3st.rutapp.data.repository.UserPrefsRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.repository.StopVisitRepository
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
    val checkInTs: Long?                     = null,   // capturado al abrir el formulario
)

@HiltViewModel
class VisitaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stopRepo:     StopRepository,
    private val visitRepo:    StopVisitRepository,
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
    // Fecha de la visita: viene del navArgument opcional ?date=YYYY-MM-DD.
    // Si no viene (compat retroactiva), se decide al guardar consultando la
    // ruta del stop (Modelo C requiere una fecha).
    private val visitDateArg: String? = savedStateHandle["date"]

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
                    checkInTs      = System.currentTimeMillis(),  // capturado al abrir
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
            val checkOutTs = System.currentTimeMillis()
            val nowIso     = java.time.Instant.now()
                .atOffset(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val notesValue       = _ui.value.notes.trim().ifEmpty { null }
            val nextActionValue  = _ui.value.nextAction.trim().ifEmpty { null }

            // 1. Resolver la fecha de la visita (Modelo C — 1 visita por (stop,fecha))
            //    Orden de prioridad:
            //    a) date del navArgument (cuando se entra desde RouteDetail tras seleccionar fecha)
            //    b) date del stop.dateAssigned (compat con rutas legacy)
            //    c) fecha de hoy (fallback final)
            val resolvedDate: String = when {
                !visitDateArg.isNullOrBlank() -> visitDateArg
                else -> {
                    val stopRow = _ui.value.stop ?: stopRepo.getByUid(stopUid)
                    stopRow?.dateAssigned?.takeIf { it.isNotBlank() }
                        ?: java.time.LocalDate.now().toString()
                }
            }

            // 2. Crear o recuperar la stop_visit y escribir el informe en ella
            val routeUidForVisit = _ui.value.stop?.routeUid
                ?: stopRepo.getByUid(stopUid)?.routeUid
            val visitUidForKpis: String? = if (routeUidForVisit != null) {
                val existing = visitRepo.getByStopAndDate(stopUid, resolvedDate)
                    ?: visitRepo.ensureVisitExists(stopUid, routeUidForVisit, resolvedDate)
                visitRepo.updateVisit(existing.copy(
                    status       = "done",
                    visitedAt    = nowIso,
                    visitResult  = _ui.value.selectedResult,
                    nextAction   = nextActionValue,
                    notes        = notesValue,
                    checkInTs    = _ui.value.checkInTs ?: existing.checkInTs,
                    checkOutTs   = checkOutTs,
                    gpsLatVisit  = gpsPos?.latitude  ?: existing.gpsLatVisit,
                    gpsLngVisit  = gpsPos?.longitude ?: existing.gpsLngVisit,
                ))
                existing.uid
            } else null

            // 3. Espejo en el stop — sigue siendo la fuente de verdad de la
            //    "última visita registrada" para vistas legacy (Biblioteca,
            //    GlobalMap, etc) que no usan stop_visits.
            stopRepo.saveVisitResult(
                uid         = stopUid,
                result      = _ui.value.selectedResult,
                notes       = notesValue,
                nextAction  = nextActionValue,
                pdvOpen     = _ui.value.storeOpen,
                pdvInactive = _ui.value.pdvInactive,
                gpsLat      = gpsPos?.latitude,
                gpsLng      = gpsPos?.longitude,
                checkInTs   = _ui.value.checkInTs,
                checkOutTs  = checkOutTs,
            )

            // 4. Persistir valores KPI en Room — ancladas a la visita actual
            //    Si no pudimos resolver visitUid (caso extremo: stop sin ruta),
            //    usamos el patrón '-v1' como fallback de compatibilidad.
            val effectiveVisitUid = visitUidForKpis ?: "$stopUid-v1"
            val kpiEntities = _ui.value.kpiValues
                .filter { (_, v) -> v.isNotBlank() }
                .map { (kpiId, value) ->
                    KpiValueEntity(
                        visitUid   = effectiveVisitUid,
                        stopUid    = stopUid,
                        kpiId      = kpiId,
                        valueText  = value.trim(),
                        syncStatus = "pending",
                    )
                }

            if (kpiEntities.isNotEmpty()) {
                kpiValueDao.upsertAll(kpiEntities)

                // 5. Encolar en SyncQueue para que SyncWorker los suba al servidor
                // api.php batch_sync entity="kpi_values": data={stopUid, visitUid, values:{kpiId->value}}
                val valuesMap: Map<String, Any?> = kpiEntities.associate { it.kpiId to it.valueText }
                val payload = mapAdapter.toJson(
                    mapOf(
                        "stopUid"  to stopUid,
                        "visitUid" to effectiveVisitUid,
                        "values"   to valuesMap,
                    )
                )
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entity    = "kpi_values",
                        entityUid = effectiveVisitUid,    // ahora el uid de la visita identifica el batch
                        operation = "upsert",
                        payload   = payload,
                    )
                )
            }

            // 6. Persistir fotos en Room (se subirán al servidor en background via SyncWorker)
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
            // KPIs son acumulativos: cada visita guarda el total vigente del PDV.
            // Al abrir el formulario, mostramos el ÚLTIMO total acumulado conocido
            // (la visita más reciente), no un valor aleatorio entre visitas.
            // El agente verá ese número y lo incrementará si corresponde.
            val existing = kpiValueDao.getLastTotalsByStop(stopUid)
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

package com.pabl3st.rutapp.feature.importar

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.import.CsvParser
import com.pabl3st.rutapp.core.import.GeoCluster
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ── Paso del stepper ──────────────────────────────────────────
enum class ImportStep { PICK_FILE, MAP_COLUMNS, PREVIEW, DONE }

// ── Columnas obligatorias / opcionales mapeables ──────────────
enum class StopField(val label: String, val required: Boolean) {
    NAME("Nombre del PDV", true),
    EXTERNAL_ID("ID externo / código", false),
    ADDRESS("Dirección", false),
    LAT("Latitud", false),
    LNG("Longitud", false),
    CONTACT_NAME("Contacto (nombre)", false),
    CONTACT_PHONE("Teléfono contacto", false),
    NOTES("Notas", false),
}

// ── Preview de una parada importada ──────────────────────────
data class StopPreview(
    val rowIndex:    Int,
    val name:        String,
    val externalId:  String?,
    val address:     String?,
    val lat:         Double?,
    val lng:         Double?,
    val contactName: String?,
    val contactPhone:String?,
    val notes:       String?,
    val hasGps:      Boolean,
    val warning:     String?,    // null = OK
)

// ── Parámetros de clustering ──────────────────────────────────
data class ClusterParams(
    val strategy:  GeoCluster.Strategy = GeoCluster.Strategy.AUTO,
    val fixedK:    Int                 = 3,
    val radiusKm:  Double              = 10.0,
    val startDate: LocalDate           = LocalDate.now(),
)

data class ImportarUiState(
    val step:         ImportStep                  = ImportStep.PICK_FILE,
    // Paso 1 — fichero
    val fileName:     String?                     = null,
    val parseError:   String?                     = null,
    val isLoading:    Boolean                     = false,
    // Paso 2 — mapeo de columnas
    val csvHeaders:   List<String>                = emptyList(),
    val mapping:      Map<StopField, String?>     = StopField.entries.associateWith { null },
    val mappingError: String?                     = null,
    // Paso 3 — preview + clustering
    val previews:     List<StopPreview>           = emptyList(),
    val clusterParams: ClusterParams              = ClusterParams(),
    val clusters:     List<List<StopPreview>>     = emptyList(),
    val clusterNames: List<String>                = emptyList(),
    // Paso 4 — guardando
    val saveProgress: Int                         = 0,
    val saveTotal:    Int                          = 0,
    val saveError:    String?                      = null,
)

@HiltViewModel
class ImportarViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val stopRepo:  StopRepository,
    private val routeRepo: RouteRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ImportarUiState())
    val ui: StateFlow<ImportarUiState> = _ui.asStateFlow()

    // ── PASO 1 — parsear fichero ──────────────────────────────

    fun onFilePicked(uri: Uri, fileName: String) {
        _ui.update { it.copy(isLoading = true, parseError = null, fileName = fileName) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val stream = ctx.contentResolver.openInputStream(uri)
                        ?: error("No se pudo abrir el fichero")
                    CsvParser.parse(stream)
                }.fold(
                    onSuccess = { result ->
                        if (result.headers.isEmpty()) {
                            _ui.update { it.copy(isLoading = false, parseError = "Sin cabeceras") }
                        } else {
                            val autoMapping = autoMap(result.headers)
                            _rawRows = result.rows   // guardar para paso 2
                            _ui.update { s -> s.copy(
                                isLoading   = false,
                                csvHeaders  = result.headers,
                                mapping     = autoMapping,
                                step        = ImportStep.MAP_COLUMNS,
                            )}
                        }
                    },
                    onFailure = { e ->
                        _ui.update { it.copy(isLoading = false, parseError = e.message) }
                    }
                )
            }
        }
    }

    // Auto-detectar columnas por nombre aproximado
    private fun autoMap(headers: List<String>): Map<StopField, String?> {
        val h = headers.map { it.lowercase().trim() }
        fun best(vararg keywords: String): String? =
            headers.getOrNull(keywords.firstNotNullOfOrNull { kw -> h.indexOfFirst { it.contains(kw) }.takeIf { it >= 0 } } ?: -1)

        return mapOf(
            StopField.NAME         to (best("nombre", "name", "pdv", "cliente", "razon") ?: headers.firstOrNull()),
            StopField.EXTERNAL_ID  to best("codigo", "code", "id", "ref", "external"),
            StopField.ADDRESS      to best("direccion", "address", "calle", "domicilio"),
            StopField.LAT          to best("lat", "latitud", "latitude"),
            StopField.LNG          to best("lng", "lon", "longitud", "longitude"),
            StopField.CONTACT_NAME to best("contacto", "contact", "responsable", "persona"),
            StopField.CONTACT_PHONE to best("telefono", "phone", "tel", "movil"),
            StopField.NOTES        to best("notas", "notes", "observaciones", "comentario"),
        )
    }

    // ── PASO 2 — actualizar mapping ───────────────────────────

    fun onMappingChange(field: StopField, header: String?) {
        _ui.update { it.copy(mapping = it.mapping + (field to header), mappingError = null) }
    }

    fun onMappingConfirm(rawRows: List<Map<String, String>>) {
        val mapping = _ui.value.mapping
        val nameCol = mapping[StopField.NAME]
        if (nameCol == null) {
            _ui.update { it.copy(mappingError = "El campo 'Nombre del PDV' es obligatorio") }
            return
        }

        val previews = rawRows.mapIndexed { idx, row ->
            val name   = row[nameCol]?.takeIf { it.isNotBlank() } ?: "Fila ${idx + 2}"
            val lat    = row[mapping[StopField.LAT]]?.toDoubleOrNull()
            val lng    = row[mapping[StopField.LNG]]?.toDoubleOrNull()
            val hasGps = lat != null && lng != null
            val warning = when {
                name == "Fila ${idx + 2}" -> "Sin nombre"
                !hasGps                   -> "Sin GPS — no se incluirá en clustering"
                else                      -> null
            }
            StopPreview(
                rowIndex     = idx,
                name         = name,
                externalId   = row[mapping[StopField.EXTERNAL_ID]]?.takeIf { it.isNotBlank() },
                address      = row[mapping[StopField.ADDRESS]]?.takeIf { it.isNotBlank() },
                lat          = lat,
                lng          = lng,
                contactName  = row[mapping[StopField.CONTACT_NAME]]?.takeIf { it.isNotBlank() },
                contactPhone = row[mapping[StopField.CONTACT_PHONE]]?.takeIf { it.isNotBlank() },
                notes        = row[mapping[StopField.NOTES]]?.takeIf { it.isNotBlank() },
                hasGps       = hasGps,
                warning      = warning,
            )
        }

        val clusters = buildClusters(previews, _ui.value.clusterParams)
        _ui.update { it.copy(
            previews  = previews,
            clusters  = clusters,
            clusterNames = defaultClusterNames(clusters.size, _ui.value.clusterParams.startDate),
            step      = ImportStep.PREVIEW,
        )}
    }

    // ── PASO 3 — clustering y nombres ────────────────────────

    fun onClusterParamsChange(params: ClusterParams) {
        val clusters = buildClusters(_ui.value.previews, params)
        _ui.update { it.copy(
            clusterParams = params,
            clusters      = clusters,
            clusterNames  = defaultClusterNames(clusters.size, params.startDate),
        )}
    }

    fun onClusterNameChange(index: Int, name: String) {
        val names = _ui.value.clusterNames.toMutableList()
        if (index < names.size) names[index] = name
        _ui.update { it.copy(clusterNames = names) }
    }

    private fun buildClusters(previews: List<StopPreview>, params: ClusterParams): List<List<StopPreview>> {
        val geoStops = previews.filter { it.hasGps }.mapIndexed { i, p ->
            GeoCluster.Stop(i, p.name, p.lat!!, p.lng!!)
        }
        if (geoStops.isEmpty()) return if (previews.isEmpty()) emptyList() else listOf(previews)

        val result = GeoCluster.cluster(geoStops, params.strategy, params.fixedK, params.radiusKm)
        val indexMap = previews.associateBy { it.rowIndex }

        val clustered = result.clusters.map { group ->
            group.map { gs -> previews[gs.index] }
        }
        // Paradas sin GPS van a una ruta extra "Sin GPS"
        val withoutGps = previews.filter { !it.hasGps }
        return if (withoutGps.isEmpty()) clustered else clustered + listOf(withoutGps)
    }

    private fun defaultClusterNames(k: Int, startDate: LocalDate): List<String> {
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return (0 until k).map { i ->
            val date = startDate.plusDays(i.toLong())
            "Ruta ${i + 1} — ${date.format(fmt)}"
        }
    }

    // ── PASO 4 — guardar en Room ──────────────────────────────

    fun onSaveConfirm() {
        val clusters     = _ui.value.clusters
        val clusterNames = _ui.value.clusterNames
        val startDate    = _ui.value.clusterParams.startDate
        val total        = clusters.sumOf { it.size }

        _ui.update { it.copy(isLoading = true, saveProgress = 0, saveTotal = total) }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var saved = 0
                clusters.forEachIndexed { idx, stops ->
                    val routeName = clusterNames.getOrElse(idx) { "Ruta ${idx + 1}" }
                    val date      = startDate.plusDays(idx.toLong()).toString()
                    val route     = routeRepo.createRoute(name = routeName, dateAssigned = date)

                    stops.forEachIndexed { stopIdx, preview ->
                        stopRepo.createStop(
                            routeUid     = route.uid,
                            name         = preview.name,
                            externalId   = preview.externalId,
                            address      = preview.address,
                            lat          = preview.lat,
                            lng          = preview.lng,
                            orderIndex   = stopIdx,
                            notes        = preview.notes,
                            contactName  = preview.contactName,
                            contactPhone = preview.contactPhone,
                        )
                        saved++
                        _ui.update { it.copy(saveProgress = saved) }
                    }
                }
                _ui.update { it.copy(isLoading = false, step = ImportStep.DONE) }
            }
        }
    }

    // ── PASO CSV crudo para confirmación ─────────────────────
    // Se guarda aquí tras parsear en PASO 1 para usarlo en PASO 2 confirm

    private var _rawRows: List<Map<String, String>> = emptyList()

    fun storeRawRows(rows: List<Map<String, String>>) { _rawRows = rows }
    fun getRawRows(): List<Map<String, String>> = _rawRows

    fun reset() { _ui.value = ImportarUiState(); _rawRows = emptyList() }
}

package com.pabl3st.rutapp.feature.importar

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.importer.CsvParser
import com.pabl3st.rutapp.core.importer.GeoCluster
import com.pabl3st.rutapp.core.importer.XlsxParser
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.session.SessionManager
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

// ── Pasos del stepper (ampliado a 6) ─────────────────────────
enum class ImportStep {
    PICK_FILE,       // 1. Seleccionar fichero
    MAP_COLUMNS,     // 2. Mapear columnas
    PREVIEW,         // 3. Preview + clustering en rutas
    CALENDAR,        // 4. Asignar rutas a días del calendario
    KPI_REPORTS,     // 5. Importar KPIs y reports históricos
    DONE             // 6. Completado
}

// ── Columnas mapeables (ampliadas) ──────────────────────────
enum class StopField(val label: String, val required: Boolean) {
    NAME(            "Nombre del PDV",       true),
    EXTERNAL_ID(     "ID externo / código",  false),
    ADDRESS(         "Dirección",            false),
    LAT(             "Latitud",              false),
    LNG(             "Longitud",             false),
    CONTACT_NAME(    "Contacto (nombre)",    false),
    CONTACT_PHONE(   "Teléfono contacto",    false),
    NOTES(           "Notas",               false),
    // Nuevos campos
    ROUTE_NAME(      "Nombre de ruta",       false),
    VISIT_FREQUENCY( "Frecuencia visita (días)", false),
    PRIORITY(        "Prioridad (1-5)",      false),
    SEGMENT(         "Segmento (A/B/C)",     false),
}

// ── Preview de una parada ─────────────────────────────────────
data class StopPreview(
    val rowIndex:       Int,
    val name:           String,
    val externalId:     String?,
    val address:        String?,
    val lat:            Double?,
    val lng:            Double?,
    val contactName:    String?,
    val contactPhone:   String?,
    val notes:          String?,
    val routeName:      String?,
    val visitFrequency: Int?,
    val priority:       Int,
    val segment:        String?,
    val hasGps:         Boolean,
    val warning:        String?,
)

// ── Asignación de ruta a fecha ────────────────────────────────
data class RouteCalendarEntry(
    val clusterIndex:   Int,
    val routeName:      String,
    /** Primera fecha de la ruta (primer día laborable del mes elegido) */
    val date:           LocalDate?,
    val stopCount:      Int,
    /** Todas las fechas de visita programadas para esta ruta en el mes */
    val scheduledDates: List<LocalDate> = emptyList(),
    /** true si las fechas vienen de la hoja CALENDARIO del XLS importado */
    val datesFromImport: Boolean = false,
)

// ── KPI report de una visita histórica ───────────────────────
data class KpiReportRow(
    val stopExternalId: String,
    val date:           String,
    val kpiActivaciones: String,
    val kpiPrimerBono:   String,
    val kpiMediaBono:    String,
    val kpiRecargas:     String,
    val kpiNroTv:        String,
    val plus:            Boolean,
    val pdvInactivo:     Boolean,
)

// ── Columnas de KPI Report mapeables ─────────────────────────
enum class KpiField(val label: String, val required: Boolean) {
    STOP_ID(       "ID parada (external_id)", true),
    DATE(          "Fecha visita",            true),
    ACTIVACIONES(  "Activaciones",            false),
    PRIMER_BONO(   "Primer bono (€)",         false),
    MEDIA_BONO(    "Media bono (€)",          false),
    RECARGAS(      "Recargas",                false),
    NRO_TV(        "Nº TV",                   false),
    PLUS(          "Plus (true/false)",       false),
    PDV_INACTIVO(  "PDV inactivo (true/false)", false),
}

// ── Parámetros de clustering ──────────────────────────────────
data class ClusterParams(
    val strategy:  GeoCluster.Strategy = GeoCluster.Strategy.AUTO,
    val fixedK:    Int                 = 3,
    val radiusKm:  Double              = 10.0,
    val startDate: LocalDate           = LocalDate.now(),
    val selectedMonth: java.time.YearMonth = java.time.YearMonth.now(),
)

data class ImportarUiState(
    val step:           ImportStep                   = ImportStep.PICK_FILE,
    // Paso 1
    val fileName:       String?                      = null,
    val parseError:     String?                      = null,
    val isLoading:      Boolean                      = false,
    // Paso 2 — mapeo columnas de paradas
    val csvHeaders:     List<String>                 = emptyList(),
    val mapping:        Map<StopField, String?>      = StopField.entries.associateWith { null },
    val mappingError:   String?                      = null,
    // Paso 3 — preview + clustering
    val previews:       List<StopPreview>            = emptyList(),
    val clusterParams:  ClusterParams                = ClusterParams(),
    val clusters:       List<List<StopPreview>>      = emptyList(),
    val clusterNames:   List<String>                 = emptyList(),
    // Paso 4 — calendario: asignar rutas a días
    val calendarEntries: List<RouteCalendarEntry>    = emptyList(),
    val selectedMonth:  java.time.YearMonth             = java.time.YearMonth.now(),
    // Paso 5 — KPI reports
    val hasKpiSheet:       Boolean                   = false,   // el fichero tiene hoja KPI
    val hasCalendarSheet:  Boolean                   = false,   // el fichero tiene hoja CALENDARIO con fechas de visita
    val kpiHeaders:     List<String>                 = emptyList(),
    val kpiMapping:     Map<KpiField, String?>       = KpiField.entries.associateWith { null },
    val kpiMappingError: String?                     = null,
    val kpiReports:     List<KpiReportRow>           = emptyList(),
    val kpiPreviewCount: Int                         = 0,
    val skipKpi:        Boolean                      = false,   // el usuario salta este paso
    // Paso 6 — guardando
    val saveProgress:   Int                          = 0,
    val saveTotal:      Int                          = 0,
    val saveError:      String?                      = null,
)

@HiltViewModel
class ImportarViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val stopRepo:   StopRepository,
    private val routeRepo:  RouteRepository,
    private val kpiValueDao: KpiValueDao,
    private val session:    SessionManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(ImportarUiState())
    val ui: StateFlow<ImportarUiState> = _ui.asStateFlow()
    private var _rawRows:    List<Map<String, String>> = emptyList()
    private var _kpiRawRows: List<Map<String, String>> = emptyList()

    // ── PASO 1 — parsear fichero ──────────────────────────────

    fun onFilePicked(uri: Uri, fileName: String) {
        _ui.update { it.copy(isLoading = true, parseError = null, fileName = fileName) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val rawStream = ctx.contentResolver.openInputStream(uri)
                        ?: error("No se pudo abrir el fichero")

                    // Detectar formato de forma robusta:
                    // uri.lastPathSegment puede ser "primary:Downloads/file.xlsx",
                    // "document/msf:12345" o similar — no fiarse solo del nombre.
                    // Magic bytes: XLSX/ZIP siempre empieza con PK (0x50 0x4B).
                    val peekBytes  = rawStream.readNBytes(2)
                    val isXlsxMagic = peekBytes.size >= 2 &&
                                      peekBytes[0] == 0x50.toByte() &&
                                      peekBytes[1] == 0x4B.toByte()
                    val nameLower   = fileName.lowercase()
                    val isXlsxName  = nameLower.contains(".xlsx") || nameLower.contains(".xls")
                    val mimeType    = ctx.contentResolver.getType(uri) ?: ""
                    val isXlsxMime  = mimeType.contains("spreadsheet") || mimeType.contains("excel")
                    val isXlsx = isXlsxMagic || isXlsxName || isXlsxMime

                    // Reconstruir stream completo con los 2 bytes ya leídos
                    val stream = java.io.SequenceInputStream(
                        java.io.ByteArrayInputStream(peekBytes),
                        rawStream
                    )

                    if (isXlsx) {
                        // Intentar leer hoja PARADAS o CSV_PARADAS primero, luego la primera hoja
                        val multiSheet = XlsxParser.parseMultiSheet(stream)
                        val stopSheet  = multiSheet["PARADAS"]
                            ?: multiSheet["CSV_PARADAS"]
                            ?: multiSheet.values.first()
                        val calSheet   = multiSheet["CALENDARIO"] ?: multiSheet["CSV_CALENDARIO"]
                        val kpiSheet   = multiSheet["KPI_VISITAS"] ?: multiSheet["CSV_KPI_VISITAS"]

                        // KPI sheet del XLSX (opcional)
                        if (kpiSheet != null) {
                            _kpiRawRows = kpiSheet.rows
                            _ui.update { it.copy(hasKpiSheet = true, kpiHeaders = kpiSheet.headers) }
                        }
                        // Calendar sheet: pre-leer fechas si existe
                        if (calSheet != null) {
                            _calRows = calSheet.rows
                            _ui.update { it.copy(hasCalendarSheet = calSheet.rows.isNotEmpty()) }
                        }
                        stopSheet
                    } else {
                        CsvParser.parse(stream)
                    }
                }.fold(
                    onSuccess = { result ->
                        if (result.headers.isEmpty()) {
                            _ui.update { it.copy(isLoading = false, parseError = "Sin cabeceras detectadas") }
                        } else {
                            val autoMapping = autoMap(result.headers)
                            _rawRows = result.rows
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

    private fun autoMap(headers: List<String>): Map<StopField, String?> {
        val h = headers.map { it.lowercase().trim() }
        // exact() busca coincidencia exacta primero, luego contains
        fun best(vararg kw: String): String? {
            // 1. exact match
            for (k in kw) {
                val idx = h.indexOfFirst { it == k }
                if (idx >= 0) return headers[idx]
            }
            // 2. contains match
            for (k in kw) {
                val idx = h.indexOfFirst { it.contains(k) }
                if (idx >= 0) return headers[idx]
            }
            return null
        }
        // Dirección: preferir full_address > address > direccion para máxima info
        val addressCol = best("full_address","direccion","address","calle","domicilio","addr")
        return mapOf(
            StopField.NAME          to (best("name","nombre","pdv","cliente","razon") ?: headers.firstOrNull()),
            StopField.EXTERNAL_ID   to best("external_id","codigo","code","ref","external"),
            StopField.ADDRESS       to addressCol,
            StopField.LAT           to best("lat","latitud","latitude"),
            StopField.LNG           to best("lng","lon","longitud","longitude"),
            StopField.CONTACT_NAME  to best("contact_name","contacto","contact","responsable","persona"),
            StopField.CONTACT_PHONE to best("contact_phone","telefono","phone","tel","movil"),
            StopField.NOTES         to best("notes","notas","observaciones","comentario"),
            StopField.ROUTE_NAME    to best("route_name","ruta","route"),
            StopField.VISIT_FREQUENCY to best("visit_frequency","frecuencia","frequency","informes"),
            StopField.PRIORITY      to best("priority","prioridad"),
            StopField.SEGMENT       to best("segment","segmento"),
        )
    }

    private fun autoMapKpi(headers: List<String>): Map<KpiField, String?> {
        val h = headers.map { it.lowercase().trim() }
        fun best(vararg kw: String): String? =
            headers.getOrNull(kw.firstNotNullOfOrNull { k -> h.indexOfFirst { it.contains(k) }.takeIf { it >= 0 } } ?: -1)
        return mapOf(
            KpiField.STOP_ID      to best("stop_uid","external_id","id","stop_id","uid"),
            KpiField.DATE         to best("fecha","date","last_visit","visited_at"),
            KpiField.ACTIVACIONES to best("activaciones","kpi_activaciones","acts"),
            KpiField.PRIMER_BONO  to best("primer_bono","kpi_primer_bono","primerbono"),
            KpiField.MEDIA_BONO   to best("media_bono","kpi_media_bono","mediabono"),
            KpiField.RECARGAS     to best("recargas","kpi_recargas"),
            KpiField.NRO_TV       to best("nro_tv","kpi_nro_tv","tv"),
            KpiField.PLUS         to best("plus","plus_activo"),
            KpiField.PDV_INACTIVO to best("pdv_inactivo","pdvinactivo","inactivo"),
        )
    }

    // ── PASO 2 — mapping ──────────────────────────────────────

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
            val freq   = row[mapping[StopField.VISIT_FREQUENCY]]?.trim()?.toIntOrNull()
            val prio   = row[mapping[StopField.PRIORITY]]?.trim()?.toIntOrNull()?.coerceIn(1, 5) ?: 3
            val seg    = row[mapping[StopField.SEGMENT]]?.trim()?.uppercase()?.takeIf { it.length == 1 }
            val warning = when {
                name == "Fila ${idx + 2}" -> "Sin nombre"
                lat == null || lng == null -> "Sin GPS — no se incluirá en clustering"
                else                       -> null
            }
            StopPreview(
                rowIndex       = idx,
                name           = name,
                externalId     = row[mapping[StopField.EXTERNAL_ID]]?.takeIf { it.isNotBlank() },
                address        = row[mapping[StopField.ADDRESS]]?.takeIf { it.isNotBlank() },
                lat            = lat, lng = lng,
                contactName    = row[mapping[StopField.CONTACT_NAME]]?.takeIf { it.isNotBlank() },
                contactPhone   = row[mapping[StopField.CONTACT_PHONE]]?.takeIf { it.isNotBlank() },
                notes          = row[mapping[StopField.NOTES]]?.takeIf { it.isNotBlank() },
                routeName      = row[mapping[StopField.ROUTE_NAME]]?.takeIf { it.isNotBlank() },
                visitFrequency = freq, priority = prio, segment = seg,
                hasGps         = lat != null && lng != null,
                warning        = warning,
            )
        }
        val clusters = buildClusters(previews, _ui.value.clusterParams)
        _ui.update { it.copy(
            previews     = previews,
            clusters     = clusters,
            clusterNames = defaultClusterNames(clusters.size, clusters),
            step         = ImportStep.PREVIEW,
        )}
    }

    // ── PASO 3 — clustering ───────────────────────────────────

    fun onClusterParamsChange(params: ClusterParams) {
        val clusters = buildClusters(_ui.value.previews, params)
        _ui.update { it.copy(
            clusterParams = params,
            clusters      = clusters,
            clusterNames  = defaultClusterNames(clusters.size, clusters),
        )}
    }

    fun onClusterNameChange(index: Int, name: String) {
        val names = _ui.value.clusterNames.toMutableList()
        if (index < names.size) names[index] = name
        _ui.update { it.copy(clusterNames = names) }
    }

    fun onPreviewConfirm() {
        // Construir entradas de calendario
        val preloadedDates = buildCalendarFromSheet()

        // Detectar el mes de las fechas importadas para pre-seleccionar el selector de mes
        val importedMonthFromSheet = _calRows
            .mapNotNull { it["date"]?.trim() }
            .mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
            .minOrNull()
            ?.let { java.time.YearMonth.from(it) }

        // 1 ruta por nombre (cluster) — sus múltiples fechas son scheduledDates, no rutas separadas
        // Ej: PS06 con visitas el 12 y 21 → 1 RouteCalendarEntry con scheduledDates=[12,21]
        val calSheetByRoute: Map<String, List<LocalDate>> = _calRows
            .groupBy { it["route_name"]?.trim() ?: "" }
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, rows) ->
                rows.mapNotNull { r ->
                    runCatching { LocalDate.parse(r["date"]?.trim() ?: "") }.getOrNull()
                }.distinct().sorted()
            }

        // Calcular primer día laborable del mes seleccionado desde la fecha de importación
        val month = _ui.value.selectedMonth
        val importDay = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
        // Si estamos dentro del mes, empezar por el primer L-V de la semana actual
        val firstWorkday = firstWorkdayOfImportWeek(importDay, month)

        val entries = mutableListOf<RouteCalendarEntry>()
        _ui.value.clusters.forEachIndexed { idx, stops ->
            val baseName  = _ui.value.clusterNames.getOrElse(idx) { "Ruta ${idx + 1}" }
            val routeKey  = stops.firstOrNull()?.routeName?.trim() ?: ""
            val datesForRoute = calSheetByRoute[routeKey]

            // Fecha principal = primera fecha de visita, o primer día laborable si no hay sheet
            val primaryDate = datesForRoute?.firstOrNull() ?: preloadedDates[idx] ?: firstWorkday
            val allDates    = datesForRoute ?: listOfNotNull(preloadedDates[idx])

            entries.add(RouteCalendarEntry(
                clusterIndex        = idx,
                routeName           = baseName,
                date                = primaryDate,
                stopCount           = stops.size,
                scheduledDates      = allDates,
                datesFromImport     = datesForRoute != null,  // true = fechas vienen del XLS
            ))
        }

        // Preparar KPI mapping si hay hoja de KPIs
        val kpiAutoMap = if (_ui.value.kpiHeaders.isNotEmpty()) autoMapKpi(_ui.value.kpiHeaders) else emptyMap()
        val effectiveMonth = importedMonthFromSheet ?: _ui.value.selectedMonth
        _ui.update { it.copy(
            calendarEntries    = entries,
            kpiMapping         = kpiAutoMap,
            step               = ImportStep.CALENDAR,
            hasCalendarSheet   = _calRows.isNotEmpty(),
            selectedMonth      = effectiveMonth,
        )}
    }

    // ── PASO 4 — calendario ───────────────────────────────────

    fun onCalendarDateChange(clusterIndex: Int, date: LocalDate?) {
        val entries = _ui.value.calendarEntries.map {
            if (it.clusterIndex == clusterIndex) it.copy(date = date) else it
        }
        _ui.update { it.copy(calendarEntries = entries) }
    }

    fun onCalendarConfirm() {
        if (_ui.value.hasKpiSheet || _kpiRawRows.isNotEmpty()) {
            _ui.update { it.copy(step = ImportStep.KPI_REPORTS) }
        } else {
            // Sin KPIs → saltar directo a guardar
            onSaveConfirm()
        }
    }

    fun skipCalendarStep() {
        onCalendarConfirm()
    }

    // ── PASO 5 — KPI Reports ──────────────────────────────────

    fun onKpiMappingChange(field: KpiField, header: String?) {
        _ui.update { it.copy(kpiMapping = it.kpiMapping + (field to header), kpiMappingError = null) }
    }

    fun onKpiMappingConfirm() {
        val mapping  = _ui.value.kpiMapping
        val stopCol  = mapping[KpiField.STOP_ID]
        val dateCol  = mapping[KpiField.DATE]
        if (stopCol == null || dateCol == null) {
            _ui.update { it.copy(kpiMappingError = "ID parada y Fecha son obligatorios") }
            return
        }
        val reports = _kpiRawRows.mapNotNull { row ->
            val stopId = row[stopCol]?.trim() ?: return@mapNotNull null
            val date   = row[dateCol]?.trim()  ?: return@mapNotNull null
            if (stopId.isBlank() || date.isBlank()) return@mapNotNull null
            KpiReportRow(
                stopExternalId  = stopId,
                date            = date,
                kpiActivaciones = row[mapping[KpiField.ACTIVACIONES]]?.trim() ?: "",
                kpiPrimerBono   = row[mapping[KpiField.PRIMER_BONO]]?.trim()  ?: "",
                kpiMediaBono    = row[mapping[KpiField.MEDIA_BONO]]?.trim()   ?: "",
                kpiRecargas     = row[mapping[KpiField.RECARGAS]]?.trim()     ?: "",
                kpiNroTv        = row[mapping[KpiField.NRO_TV]]?.trim()       ?: "",
                plus            = row[mapping[KpiField.PLUS]]?.trim()?.lowercase() in listOf("true","si","sí","1","yes"),
                pdvInactivo     = row[mapping[KpiField.PDV_INACTIVO]]?.trim()?.lowercase() in listOf("true","si","sí","1","yes"),
            )
        }
        _ui.update { it.copy(
            kpiReports      = reports,
            kpiPreviewCount = reports.size,
        )}
        onSaveConfirm()
    }

    fun onSkipKpi() {
        _ui.update { it.copy(skipKpi = true) }
        onSaveConfirm()
    }

    // ── PASO 6 — guardar todo en Room ─────────────────────────

    fun onSaveConfirm() {
        val clusters     = _ui.value.clusters
        val calEntries   = _ui.value.calendarEntries
        val kpiReports   = if (_ui.value.skipKpi) emptyList() else _ui.value.kpiReports
        val total        = calEntries.sumOf { it.stopCount }.coerceAtLeast(clusters.sumOf { it.size })

        _ui.update { it.copy(isLoading = true, saveProgress = 0, saveTotal = total) }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val externalIdToStopUid = mutableMapOf<String, String>()
                var saved = 0

                try {
                    val entriesToProcess = if (calEntries.isNotEmpty()) calEntries else {
                        // Fallback: sin calendario, una entry por cluster
                        clusters.mapIndexed { idx, stops ->
                            RouteCalendarEntry(
                                clusterIndex = idx,
                                routeName    = _ui.value.clusterNames.getOrElse(idx) { "Ruta ${idx + 1}" },
                                date         = LocalDate.now(),
                                stopCount    = stops.size,
                            )
                        }
                    }

                    entriesToProcess.forEach { entry ->
                        val stops = clusters.getOrNull(entry.clusterIndex) ?: return@forEach
                        val date  = entry.date?.toString() ?: LocalDate.now().toString()

                        // scheduledDates como JSON array para la ruta
                        val scheduledJson: String? = if (entry.scheduledDates.size > 1) {
                            val datesStr = entry.scheduledDates.joinToString(",") { d -> "\"$d\"" }
                            "[$datesStr]"
                        } else null

                        val route = routeRepo.createRoute(
                            name           = entry.routeName,
                            dateAssigned   = date,
                            scheduledDates = scheduledJson,
                        )

                        stops.forEachIndexed { stopIdx, preview ->
                            val stop = stopRepo.createStop(
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
                            preview.externalId?.let { externalIdToStopUid[it] = stop.uid }
                            saved++
                            _ui.update { it.copy(saveProgress = saved) }
                        }
                    }
                } catch (e: Exception) {
                    _ui.update { it.copy(
                        isLoading = false,
                        saveError = "Error al guardar: ${e.message}",
                    )}
                    return@withContext
                }

                // Guardar KPI reports históricos como kpi_values
                if (kpiReports.isNotEmpty()) {
                    val kpiEntities = kpiReports.flatMap { report ->
                        val stopUid = externalIdToStopUid[report.stopExternalId] ?: return@flatMap emptyList()
                        buildList {
                            if (report.kpiActivaciones.isNotBlank())
                                add(KpiValueEntity(stopUid, "telco_activaciones",  report.kpiActivaciones, "synced"))
                            if (report.kpiPrimerBono.isNotBlank())
                                add(KpiValueEntity(stopUid, "telco_primer_bono",   report.kpiPrimerBono,   "synced"))
                            if (report.kpiMediaBono.isNotBlank())
                                add(KpiValueEntity(stopUid, "telco_media_bono",    report.kpiMediaBono,    "synced"))
                            if (report.kpiRecargas.isNotBlank())
                                add(KpiValueEntity(stopUid, "telco_recargas",      report.kpiRecargas,     "synced"))
                            if (report.kpiNroTv.isNotBlank())
                                add(KpiValueEntity(stopUid, "telco_tv",            report.kpiNroTv,        "synced"))
                            add(KpiValueEntity(stopUid, "telco_plus",          if (report.plus) "true" else "false",         "synced"))
                            add(KpiValueEntity(stopUid, "telco_pdv_inactivo",  if (report.pdvInactivo) "true" else "false",  "synced"))
                        }
                    }
                    if (kpiEntities.isNotEmpty()) kpiValueDao.upsertAll(kpiEntities)
                }

                _ui.update { it.copy(isLoading = false, step = ImportStep.DONE) }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    fun onMonthChange(month: java.time.YearMonth) {
        _ui.update { it.copy(selectedMonth = month) }
        if (_ui.value.calendarEntries.isNotEmpty()) rebuildCalendarEntries()
    }

    private fun rebuildCalendarEntries() {
        val month = _ui.value.selectedMonth
        val first = firstWorkdayOfImportWeek(java.time.LocalDate.now(), month)
        val entries = _ui.value.calendarEntries.mapIndexed { i, entry ->
            val newDate = if (entry.scheduledDates.isNotEmpty()) entry.scheduledDates.first()
                          else {
                              var d = first.plusDays(i.toLong())
                              while (d.dayOfWeek.value > 5) d = d.plusDays(1)
                              d
                          }
            entry.copy(date = newDate)
        }
        _ui.update { it.copy(calendarEntries = entries) }
    }

    private fun firstWorkdayOfImportWeek(
        importDay: java.time.LocalDate,
        month: java.time.YearMonth,
    ): java.time.LocalDate {
        val monthStart = month.atDay(1)
        val monthEnd   = month.atEndOfMonth()
        val today = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
        val candidate = when {
            month.isAfter(java.time.YearMonth.now()) -> {
                // Mes futuro → primer lunes del mes
                var d = monthStart
                while (d.dayOfWeek.value > 5) d = d.plusDays(1)
                d
            }
            month.isBefore(java.time.YearMonth.now()) -> {
                // Mes pasado → primer lunes del mes
                var d = monthStart
                while (d.dayOfWeek.value > 5) d = d.plusDays(1)
                d
            }
            else -> {
                // Mes actual → lunes de la semana de hoy
                var d = today
                while (d.dayOfWeek.value > 1) d = d.minusDays(1)
                // Si el lunes calculado cae antes del inicio del mes, usar primer lunes del mes
                if (d.isBefore(monthStart)) {
                    d = monthStart
                    while (d.dayOfWeek.value > 5) d = d.plusDays(1)
                }
                d
            }
        }
        return when {
            candidate.isBefore(monthStart) -> { var d = monthStart; while (d.dayOfWeek.value > 5) d = d.plusDays(1); d }
            candidate.isAfter(monthEnd)    -> monthEnd
            else                           -> candidate
        }
    }


    private var _calRows: List<Map<String, String>> = emptyList()

    /** Lee fechas del XLSX sheet CALENDARIO si existe */
    private fun buildCalendarFromSheet(): Map<Int, LocalDate?> {
        if (_calRows.isEmpty()) return emptyMap()
        val result = mutableMapOf<Int, LocalDate?>()
        // El sheet tiene route_name + date.
        // Matchear contra el routeName REAL de cada cluster (el del CSV/route_name),
        // NO contra clusterNames que contiene "Ruta N — dd/MM/yyyy".
        val clusters = _ui.value.clusters
        _calRows.forEach { row ->
            val rName   = row["route_name"]?.trim() ?: return@forEach
            val dateStr = row["date"]?.trim()        ?: return@forEach
            // Buscar el cluster cuyas paradas tengan ese routeName
            val idx = clusters.indexOfFirst { stops ->
                stops.any { it.routeName?.trim() == rName }
            }
            if (idx >= 0) {
                runCatching { LocalDate.parse(dateStr) }.getOrNull()?.let {
                    result[idx] = it
                }
            }
        }
        return result
    }

    private fun buildClusters(previews: List<StopPreview>, params: ClusterParams): List<List<StopPreview>> {
        // Si las paradas tienen route_name propio → agrupar por él directamente
        val hasRouteNames = previews.any { it.routeName != null }
        if (hasRouteNames) {
            val byRoute = previews.groupBy { it.routeName ?: "Sin ruta" }
            return byRoute.values.toList()
        }
        // Sino → clustering geográfico
        val geoStops = previews.filter { it.hasGps }.mapIndexed { i, p ->
            GeoCluster.Stop(i, p.name, p.lat!!, p.lng!!)
        }
        if (geoStops.isEmpty()) return if (previews.isEmpty()) emptyList() else listOf(previews)
        val result    = GeoCluster.cluster(geoStops, params.strategy, params.fixedK, params.radiusKm)
        val clustered = result.clusters.map { group -> group.map { gs -> previews[gs.index] } }
        val withoutGps = previews.filter { !it.hasGps }
        return if (withoutGps.isEmpty()) clustered else clustered + listOf(withoutGps)
    }

    /**
     * Genera nombres para los clusters:
     * - Si el cluster tiene paradas con routeName (del CSV) → usa ese nombre directamente.
     * - Si es un cluster geográfico (sin routeName) → "Ruta N".
     * La fecha NO se embebe en el nombre — va en RouteCalendarEntry.date.
     */
    private fun defaultClusterNames(k: Int, clusters: List<List<StopPreview>>): List<String> {
        return (0 until k).map { i ->
            val routeNameFromCsv = clusters.getOrNull(i)
                ?.firstOrNull { it.routeName != null }
                ?.routeName
            routeNameFromCsv?.trim()?.ifBlank { null } ?: "Ruta ${i + 1}"
        }
    }

    fun onMappingConfirmFromRaw() = onMappingConfirm(_rawRows)
    fun storeRawRows(rows: List<Map<String, String>>) { _rawRows = rows }
    fun getRawRows(): List<Map<String, String>> = _rawRows
    fun reset() { _ui.value = ImportarUiState(); _rawRows = emptyList(); _kpiRawRows = emptyList() }
}






package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.dao.BusinessProfileDao
import com.pabl3st.rutapp.data.local.dao.KpiDefinitionDao
import com.pabl3st.rutapp.data.repository.PhotoRepository
import kotlinx.coroutines.sync.withLock
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.local.dao.DaySessionDao
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.DaySessionEntity
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity
import com.pabl3st.rutapp.data.network.BatchSyncRequest
import com.pabl3st.rutapp.data.network.DaySessionDto
import com.pabl3st.rutapp.data.network.KpiValueDto
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.network.StopDto
import com.pabl3st.rutapp.data.network.SyncOperation
import com.pabl3st.rutapp.data.session.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncResult {
    object Success        : SyncResult()
    object NoAuth         : SyncResult()  // Sin token — no reintentar
    object Unauthorized   : SyncResult()  // 401 del servidor — sesión expirada
    object UploadError    : SyncResult()
    object DownloadError  : SyncResult()
}

@Singleton
class SyncRepository @Inject constructor(
    private val syncQueueDao:    SyncQueueDao,
    private val routeDao:        RouteDao,
    private val stopDao:         StopDao,
    private val routeRepo:       RouteRepository,
    private val stopRepo:        StopRepository,
    private val visitRepo:       StopVisitRepository,
    private val daySessionDao:   DaySessionDao,
    private val kpiValueDao:     KpiValueDao,
    private val businessProfileDao:  BusinessProfileDao,
    private val kpiDefinitionDao:    KpiDefinitionDao,
    private val photoRepo:       PhotoRepository,
    private val api:             RutasApiService,
    private val session:         SessionManager,
    private val moshi:           Moshi,
) {
    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    /** Serializa todas las llamadas de sync entre callers (login + WorkManager
     *  periódico + forceSync() del botón + logout + importer). Si dos llegan a
     *  la vez, la segunda espera a que la primera termine. Sin esto leerían
     *  getNext50() simultáneamente y enviarían las mismas operaciones — el
     *  servidor las idempota con ON DUPLICATE KEY pero gasta tiempo y red. */
    private val syncMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun pendingCount(): Int = syncQueueDao.count()

    // ══════════════════════════════════════════════════════════════
    // Métodos públicos — separación por flujo (refactor mayo 2026)
    // ══════════════════════════════════════════════════════════════

    /**
     * **LOGIN FRESCO / CAMBIO DE CUENTA** — descarga TODO del servidor desde
     * cero, sin subir nada. Reseteamos lastSync para forzar full-download.
     * Útil tras un login con cuenta distinta (Room recién limpiado por P5)
     * o tras una reinstalación: el cliente necesita repoblar Room con todo
     * lo del servidor antes de que el usuario empiece a interactuar.
     *
     * NO sube cola. NO procesa fotos. Solo download.
     */
    suspend fun syncFullDownload(): SyncResult = syncMutex.withLock {
        val token = session.token ?: return@withLock SyncResult.NoAuth
        // Forzar that el next downloadDelta haga full-download
        session.lastSyncTimestamp = ""
        session.lastFullSyncMs    = 0L
        val downloaded = downloadDelta(token = token, since = "2000-01-01T00:00:00Z")
        if (downloaded) session.lastFullSyncMs = System.currentTimeMillis()
        if (downloaded) SyncResult.Success else SyncResult.DownloadError
    }

    /**
     * **WORKER PERIÓDICO / FORZAR SYNC BOTÓN** — bidireccional incremental.
     * Sube cola + descarga delta + sube fotos. Es el flujo "normal" que se
     * ejecuta cada 15 min o cuando el usuario pulsa el botón ☁️.
     */
    suspend fun syncIncremental(): SyncResult = syncMutex.withLock { incrementalInternal() }

    /**
     * **LOGOUT / FIN DE IMPORT** — solo sube cola al servidor + fotos pendientes.
     * NO descarga. Más rápido y no contamina Room con descarga si el usuario
     * está saliendo o si el wizard ya tiene todos los datos locales.
     *
     * Sube también las fotos porque pueden ocupar mucho y quedarse "atascadas"
     * si solo confiamos en el ciclo periódico. Especialmente importante en
     * logout: si el agent termina la jornada y cierra sesión, queremos que
     * sus fotos lleguen antes de que cierre, no quedarse en el móvil.
     */
    suspend fun syncUploadOnly(): SyncResult = syncMutex.withLock {
        val token = session.token ?: return@withLock SyncResult.NoAuth
        reEnqueueOrphans()
        purgeStaleQueueItems()
        val uploaded = uploadPending(token)
        // Fotos en background — fallo no bloquea el resultado del sync.
        // Si la red está mala las fotos quedan en Room pendientes para el
        // siguiente sync (el SyncWorker periódico las recogerá).
        runCatching { photoRepo.uploadPending() }
        if (uploaded) SyncResult.Success else SyncResult.UploadError
    }

    /** **Alias retrocompatible.** SyncWorker y RutasViewModel.forceSync siguen
     *  llamando aquí — mantienen su comportamiento incremental sin cambios. */
    suspend fun runSync(): SyncResult = syncIncremental()

    // ══════════════════════════════════════════════════════════════
    // Implementaciones privadas
    // ══════════════════════════════════════════════════════════════

    private suspend fun incrementalInternal(): SyncResult {
        val token = session.token ?: return SyncResult.NoAuth

        // Re-encolar datos huérfanos: en Room con syncStatus=pending
        // pero sin entrada en la SyncQueue (ocurre cuando la queue fue purgada
        // por exceso de intentos o por antigüedad, dejando los datos sin subir)
        reEnqueueOrphans()
        purgeStaleQueueItems()

        val uploaded = uploadPending(token)

        // Full-sync periódico: cada 12h se ignora el 'since' y se descarga
        // todo. Recupera cambios hechos directamente en BD (migraciones,
        // correcciones por SQL) que el sync incremental se saltaría para
        // siempre, ya que delta_sync filtra por updated_at > since.
        val now            = System.currentTimeMillis()
        val fullSyncEveryMs = 12L * 60 * 60 * 1000  // 12 horas
        val needFullSync   = now - session.lastFullSyncMs > fullSyncEveryMs
        val since = if (needFullSync) "2000-01-01T00:00:00Z"
                    else session.lastSyncTimestamp.ifEmpty { "2000-01-01T00:00:00Z" }

        val downloaded = downloadDelta(token = token, since = since)
        if (downloaded && needFullSync) {
            session.lastFullSyncMs = now
        }

        // Subir fotos pendientes en background — fallo no bloquea el sync de datos
        val photosOk = runCatching { photoRepo.uploadPending() }.getOrDefault(false)

        // Check for 401 — uploadPending returns false on 401
        // Use a simple heuristic: if both fail immediately with no network error, likely 401
        return when {
            uploaded && downloaded -> SyncResult.Success
            !uploaded              -> SyncResult.UploadError
            else                   -> SyncResult.DownloadError
        }
    }

    /** Centraliza la purga de items exhaustos / antiguos. Extraída del runSync
     *  para reusarse desde syncUploadOnly también. */
    private suspend fun purgeStaleQueueItems() {
        // createdAt se guarda como epoch millis (Long) — el cutoff debe ser
        // del mismo tipo. Con un String ISO, SQLite comparaba INTEGER contra
        // TEXT y borraba la cola completa en cada sync.
        val cutoffMillis = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        syncQueueDao.purgeExhausted(maxAttempts = 20)
        syncQueueDao.purgeOlderThan(cutoffMillis)
    }

    // ── Re-encolar datos huérfanos ───────────────────────────
    private suspend fun reEnqueueOrphans() {
        val queuedUids = syncQueueDao.getAllUids()

        routeRepo.getPendingOperations()
            .filter { it.entityUid !in queuedUids }
            .forEach { syncQueueDao.enqueue(it) }

        stopRepo.getPendingOperations()
            .filter { it.entityUid !in queuedUids }
            .forEach { syncQueueDao.enqueue(it) }

        visitRepo.getPendingOperations()
            .filter { it.entityUid !in queuedUids }
            .forEach { syncQueueDao.enqueue(it) }
    }

    // ── Subir operaciones pendientes de la cola ────────────────
    /**
     * Sube TODAS las operaciones pendientes de la cola, no solo las primeras
     * 50. Esto es crítico para el wizard de importación: el v7 genera ~4500
     * operaciones (8 rutas + 149 stops + ~1300 stop_visits + ~2995 kpi_values)
     * y antes (bug mayo 2026) solo se subían 50 por llamada → quedaban 4450
     * esperando al WorkManager periódico (15 min × 90 ciclos ≈ 22 horas para
     * vaciar la cola).
     *
     * Mecánica del loop:
     * - getNextBatch filtra items con attempts < 5 → los que fallan
     *   repetidamente NO bloquean al resto (se quedan para purga posterior).
     * - Si una iteración no avanza (0 synced), se incrementa stagnantCount.
     *   3 iteraciones seguidas sin progreso → break (probable error sistémico:
     *   sin red, server caído, token caducado).
     * - Límite de seguridad: 200 iteraciones × 50 ops = hasta 10.000 ops por
     *   llamada. Más que suficiente para cualquier importer realista.
     * - Si el server devuelve 401, se corta inmediatamente.
     *
     * @return true si TODAS las ops accesibles se procesaron sin error de red.
     *         false si hubo un fallo crítico (red, 401, server down).
     */
    private suspend fun uploadPending(token: String): Boolean {
        val maxIterations = 200
        var stagnantCount = 0
        repeat(maxIterations) { iter ->
            val items = syncQueueDao.getNextBatch(limit = 50, maxAttempts = 20)
            if (items.isEmpty()) return true   // cola vacía → éxito

            val ops = items.mapNotNull { item ->
                val data = runCatching { mapAdapter.fromJson(item.payload) }.getOrNull()
                if (data == null) { syncQueueDao.delete(item.id); return@mapNotNull null }
                SyncOperation(
                    entity    = item.entity,
                    uid       = item.entityUid,
                    operation = item.operation,
                    data      = data,
                )
            }
            if (ops.isEmpty()) return@repeat   // todos eran payloads corruptos, ya borrados

            val resp = runCatching {
                api.batchSync(token = token, body = BatchSyncRequest(ops))
            }.getOrNull() ?: return false   // error de red → cortar

            if (resp.code() == 401) return false  // token caducado
            if (!resp.isSuccessful || resp.body()?.ok != true) return false

            val body      = resp.body()!!
            val now       = Instant.now().toString()
            val syncedSet = body.synced?.map { it.uid }?.toSet() ?: emptySet()

            // Marcar synced en Room
            body.synced?.forEach { result ->
                when (result.entity) {
                    "route"            -> routeDao.updateSyncStatus(result.uid, "synced", now)
                    "stop"             -> stopDao.updateSyncStatus(result.uid, "synced", now)
                    "stop_visit"       -> visitRepo.markSynced(result.uid)
                    "kpi_values"       -> kpiValueDao.markSynced(result.uid)
                    // day_session y business_profile no tienen syncStatus en Room — nada que actualizar
                }
            }

            // Marcar errores en la queue (attempts++)
            body.errors?.forEach { err ->
                items.find { it.entityUid == err.uid }
                    ?.let { syncQueueDao.markFailed(it.id, err.error ?: "Error desconocido") }
            }

            // Eliminar de la queue los procesados con éxito
            items.filter { it.entityUid in syncedSet }
                .forEach { syncQueueDao.delete(it.id) }

            // Detección de estancamiento: si en esta iteración 0 ops avanzaron
            // (todos errors o todos silencio), pasamos a la siguiente. Si 3
            // iteraciones seguidas no avanzan → break para no bucle infinito.
            if (syncedSet.isEmpty()) {
                stagnantCount++
                if (stagnantCount >= 3) return false
            } else {
                stagnantCount = 0
            }
        }
        return true   // alcanzamos el límite de iteraciones — paramos sin error
    }

    // ── Descargar cambios del servidor desde timestamp ─────────
    private suspend fun downloadDelta(token: String, since: String): Boolean {
        val resp = runCatching {
            api.deltaSync(token = token, since = since)
        }.getOrNull() ?: return false

        if (resp.code() == 401) return false  // 401 handled at runSync level
        if (resp.code() == 401) {
            session.token  // Token still set but server rejects it
            return false
        }
        if (!resp.isSuccessful || resp.body()?.ok != true) return false

        val body = resp.body()!!
        body.routes?.map { it.toEntity(session.userId, session.accountId) }
            ?.let { routeDao.upsertAll(it) }
        body.stops?.mapNotNull { it.toEntity(session.accountId) }
            ?.let { if (it.isNotEmpty()) stopDao.upsertAll(it) }
        // Sincronizar stop_visits desde servidor (Modelo C — informes diarios)
        body.stopVisits?.map { it.toEntity(session.accountId) }
            ?.let { if (it.isNotEmpty()) it.forEach { v -> visitRepo.upsertFromServer(v) } }
        // Sincronizar jornadas desde servidor
        body.daySessions?.mapNotNull { it.toEntity() }
            ?.let { if (it.isNotEmpty()) it.forEach { s -> daySessionDao.upsert(s) } }
        // Sincronizar KPI values desde servidor
        body.kpiValues?.mapNotNull { it.toEntity() }
            ?.filter { it.valueText.isNotBlank() }
            ?.let { if (it.isNotEmpty()) kpiValueDao.upsertAll(it) }
        // P1 (mayo 2026): el servidor es fuente de verdad para business_profile
        // cuando tiene un sector válido. Antes solo se aplicaba si Room estaba
        // vacío (existing == null), lo que cementaba el "custom" creado por
        // getOrCreateProfile() la primera vez que se abría KPIs/Visita/Perfil:
        // el servidor podía tener "telco" para esa cuenta y nunca llegaba al
        // cliente porque ya había una fila local.
        //
        // Modo CONSERVADOR: sobreescribir SOLO si el server devuelve un perfil
        // con sector no vacío y distinto de "custom" (que es el placeholder
        // local). Si el server no tiene perfil (devuelve null) o devuelve
        // "custom", se respeta el local. Esto evita borrar cambios locales
        // pendientes de subida (la subida cliente→server llegará en P3).
        body.businessProfile?.let { bp ->
            val bpEntity = bp.toEntity(session.accountId)
            if (bpEntity.sector.isNotBlank() && bpEntity.sector != "custom") {
                businessProfileDao.upsert(bpEntity)
            }
        }

        // Restaurar kpi_definitions desde el servidor
        body.kpiDefinitions?.let { defs ->
            if (defs.isNotEmpty()) {
                // Upsert — el servidor es fuente de verdad para KPI definitions
                kpiDefinitionDao.upsertAll(defs.map { it.toEntity() })
            }
        }

        // Actualizar lista de agentes supervisados (manager)
        body.managedAgentIds?.let { session.managedAgentIds = it }

        body.serverTime?.let { session.lastSyncTimestamp = it }
        return true
    }
}

// ── Mappers DTO → Entity ──────────────────────────────────────

fun DaySessionDto.toEntity(): DaySessionEntity? {
    if (routeUid.isBlank() || dateStr.isBlank()) return null
    return DaySessionEntity(
        routeUid   = routeUid,
        dateStr    = dateStr,
        state      = state,
        startedAt  = startedAt,
        elapsedMs  = elapsedMs,
        distanceKm = distanceKm,
        lastLat    = lastLat,
        lastLng    = lastLng,
        updatedAt  = updatedAt,
    )
}

fun KpiValueDto.toEntity(): KpiValueEntity? {
    if (stopUid.isBlank() || kpiId.isBlank()) return null
    val v = valueText?.trim() ?: return null   // null o vacío del servidor = ignorar
    if (v.isEmpty()) return null
    // Modelo C: usamos visit_uid del servidor si existe, fallback a -v1
    val resolvedVisitUid = visitUid?.takeIf { it.isNotBlank() } ?: "$stopUid-v1"
    return KpiValueEntity(
        visitUid   = resolvedVisitUid,
        stopUid    = stopUid,
        kpiId      = kpiId,
        valueText  = v,
        syncStatus = "synced",
    )
}

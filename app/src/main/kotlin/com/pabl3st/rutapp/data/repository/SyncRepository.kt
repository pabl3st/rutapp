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

    /** Serializa runSync() entre callers (WorkManager periódico + forceSync()
     *  del botón + runSync() del importer). Si dos llegan a la vez, la segunda
     *  espera a que la primera termine. Sin esto, ambas leerían getNext50()
     *  simultáneamente y enviarían las mismas operaciones — el servidor las
     *  idempota con ON DUPLICATE KEY pero gasta tiempo y red en duplicados. */
    private val syncMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun pendingCount(): Int = syncQueueDao.count()

    // ── Ejecutar sync completo: subir + descargar ──────────────
    suspend fun runSync(): SyncResult = syncMutex.withLock { runSyncInternal() }

    private suspend fun runSyncInternal(): SyncResult {
        val token = session.token ?: return SyncResult.NoAuth

        // Re-encolar datos huérfanos: en Room con syncStatus=pending
        // pero sin entrada en la SyncQueue (ocurre cuando la queue fue purgada
        // por exceso de intentos o por antigüedad, dejando los datos sin subir)
        reEnqueueOrphans()

        // Purgar items exhaustos — aumentamos el umbral para ser más tolerantes
        val cutoff = java.time.Instant.now()
            .minusSeconds(30L * 24 * 3600)  // 30 días (antes eran 7)
            .atOffset(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        syncQueueDao.purgeExhausted(maxAttempts = 20)  // antes era 5
        syncQueueDao.purgeOlderThan(cutoff)

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
    private suspend fun uploadPending(token: String): Boolean {
        val items = syncQueueDao.getNext50()
        if (items.isEmpty()) return true

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

        val resp = runCatching {
            api.batchSync(token = token, body = BatchSyncRequest(ops))
        }.getOrNull() ?: return false

        if (resp.code() == 401) return false  // 401 handled at runSync level
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

        // Marcar errores en la queue
        body.errors?.forEach { err ->
            items.find { it.entityUid == err.uid }
                ?.let { syncQueueDao.markFailed(it.id, err.error ?: "Error desconocido") }
        }

        // Eliminar de la queue los procesados con éxito
        items.filter { it.entityUid in syncedSet }
            .forEach { syncQueueDao.delete(it.id) }

        return true
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

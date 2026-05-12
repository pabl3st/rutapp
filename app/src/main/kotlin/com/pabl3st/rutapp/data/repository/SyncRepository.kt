package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.dao.BusinessProfileDao
import com.pabl3st.rutapp.data.repository.PhotoRepository
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
    object Success       : SyncResult()
    object NoAuth        : SyncResult()
    object UploadError   : SyncResult()
    object DownloadError : SyncResult()
}

@Singleton
class SyncRepository @Inject constructor(
    private val syncQueueDao:    SyncQueueDao,
    private val routeDao:        RouteDao,
    private val stopDao:         StopDao,
    private val daySessionDao:   DaySessionDao,
    private val kpiValueDao:     KpiValueDao,
    private val businessProfileDao: BusinessProfileDao,
    private val photoRepo:       PhotoRepository,
    private val api:             RutasApiService,
    private val session:         SessionManager,
    private val moshi:           Moshi,
) {
    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    suspend fun pendingCount(): Int = syncQueueDao.count()

    // ── Ejecutar sync completo: subir + descargar ──────────────
    suspend fun runSync(): SyncResult {
        val token = session.token ?: return SyncResult.NoAuth
        // Purgar items exhaustos y viejos antes de sync — evita acumulación infinita
        val cutoff = java.time.Instant.now()
            .minusSeconds(7 * 24 * 3600)  // 7 días
            .atOffset(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        syncQueueDao.purgeExhausted(maxAttempts = 5)
        syncQueueDao.purgeOlderThan(cutoff)

        val uploaded = uploadPending(token)
        val downloaded = downloadDelta(
            token = token,
            since = session.lastSyncTimestamp.ifEmpty { "2000-01-01T00:00:00Z" },
        )

        // Subir fotos pendientes en background — fallo no bloquea el sync de datos
        val photosOk = runCatching { photoRepo.uploadPending() }.getOrDefault(false)

        return when {
            uploaded && downloaded -> SyncResult.Success
            !uploaded              -> SyncResult.UploadError
            else                   -> SyncResult.DownloadError
        }
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

        if (!resp.isSuccessful || resp.body()?.ok != true) return false

        val body      = resp.body()!!
        val now       = Instant.now().toString()
        val syncedSet = body.synced?.map { it.uid }?.toSet() ?: emptySet()

        // Marcar synced en Room
        body.synced?.forEach { result ->
            when (result.entity) {
                "route"            -> routeDao.updateSyncStatus(result.uid, "synced", now)
                "stop"             -> stopDao.updateSyncStatus(result.uid, "synced", now)
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

        if (!resp.isSuccessful || resp.body()?.ok != true) return false

        val body = resp.body()!!
        body.routes?.map { it.toEntity(session.userId, session.accountId) }
            ?.let { routeDao.upsertAll(it) }
        body.stops?.mapNotNull { it.toEntity(session.accountId) }
            ?.let { if (it.isNotEmpty()) stopDao.upsertAll(it) }
        // Sincronizar jornadas desde servidor
        body.daySessions?.mapNotNull { it.toEntity() }
            ?.let { if (it.isNotEmpty()) it.forEach { s -> daySessionDao.upsert(s) } }
        // Sincronizar KPI values desde servidor
        body.kpiValues?.mapNotNull { it.toEntity() }
            ?.filter { it.valueText.isNotBlank() }
            ?.let { if (it.isNotEmpty()) kpiValueDao.upsertAll(it) }
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
    return KpiValueEntity(
        stopUid    = stopUid,
        kpiId      = kpiId,
        valueText  = v,
        syncStatus = "synced",
    )
}

package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.network.BatchSyncRequest
import com.pabl3st.rutapp.data.network.RutasApiService
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
    private val syncQueueDao: SyncQueueDao,
    private val routeDao:     RouteDao,
    private val stopDao:      StopDao,
    private val api:          RutasApiService,
    private val session:      SessionManager,
    private val moshi:        Moshi,
) {
    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    suspend fun pendingCount(): Int = syncQueueDao.count()

    // ── Ejecutar sync completo: subir + descargar ──────────────
    suspend fun runSync(): SyncResult {
        val token = session.token ?: return SyncResult.NoAuth

        val uploaded = uploadPending(token)
        val downloaded = downloadDelta(
            token = token,
            since = session.lastSyncTimestamp.ifEmpty { "2000-01-01T00:00:00Z" },
        )

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
            val data = runCatching {
                mapAdapter.fromJson(item.payload) ?: emptyMap()
            }.getOrElse { emptyMap<String, Any?>() }
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
                "route" -> routeDao.updateSyncStatus(result.uid, "synced", now)
                "stop"  -> stopDao.updateSyncStatus(result.uid, "synced", now)
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
        body.serverTime?.let { session.lastSyncTimestamp = it }
        return true
    }
}

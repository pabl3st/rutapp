package com.pabl3st.rutapp.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.pabl3st.rutapp.data.local.dao.StopVisitDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.StopVisitEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.session.SessionManager
import com.pabl3st.rutapp.sync.SyncWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Visitas a un stop (1 por fecha programada).
 *
 * Modelo C — informes diarios independientes:
 * Cada (stopUid, visitDate) tiene como mucho una StopVisitEntity.
 * `ensureVisitExists()` es idempotente: si ya existe, devuelve la existente.
 */
@Singleton
class StopVisitRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val visitDao:     StopVisitDao,
    private val syncQueueDao: SyncQueueDao,
    private val session:      SessionManager,
    private val moshi:        Moshi,
) {
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private fun nowIso(): String = Instant.now().atOffset(ZoneOffset.UTC).format(isoFormatter)

    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    fun observeByRouteAndDate(routeUid: String, date: String): Flow<List<StopVisitEntity>> =
        visitDao.observeByRouteAndDate(routeUid, date)

    fun observeByStop(stopUid: String): Flow<List<StopVisitEntity>> =
        visitDao.observeByStop(stopUid)

    suspend fun getByStopAndDate(stopUid: String, date: String): StopVisitEntity? =
        visitDao.getByStopAndDate(stopUid, date)

    suspend fun getByUid(uid: String): StopVisitEntity? = visitDao.getByUid(uid)

    suspend fun getDistinctDatesByRoute(routeUid: String): List<String> =
        visitDao.getDistinctDatesByRoute(routeUid)

    suspend fun getPendingSync(): List<StopVisitEntity> = visitDao.getPendingSync()

    suspend fun markSynced(uid: String) {
        visitDao.markSynced(uid, "synced", nowIso())
    }

    /**
     * Crea una stop_visit "pending" para (stop, fecha) si no existe.
     * Idempotente: re-ejecutar con los mismos parámetros no duplica nada
     * (gracias al UNIQUE INDEX en stopUid+visitDate y al check previo).
     */
    suspend fun ensureVisitExists(
        stopUid:  String,
        routeUid: String,
        date:     String,
    ): StopVisitEntity {
        visitDao.getByStopAndDate(stopUid, date)?.let { return it }
        val now = nowIso()
        val visit = StopVisitEntity(
            uid        = UUID.randomUUID().toString(),
            stopUid    = stopUid,
            routeUid   = routeUid,
            accountId  = session.accountId,
            visitDate  = date,
            createdAt  = now,
            updatedAt  = now,
            syncStatus = "pending",
        )
        visitDao.upsert(visit)
        enqueue(visit, op = "create")
        return visit
    }

    /**
     * Persiste cambios en una visita y la marca como pending para resync.
     * Devuelve la entidad actualizada.
     */
    suspend fun updateVisit(visit: StopVisitEntity): StopVisitEntity {
        val updated = visit.copy(
            updatedAt  = nowIso(),
            syncStatus = "pending",
        )
        visitDao.upsert(updated)
        enqueue(updated, op = "update")
        return updated
    }

    /** Inserta o reemplaza una visita venida del servidor (ya sincronizada). */
    suspend fun upsertFromServer(visit: StopVisitEntity) {
        visitDao.upsert(visit)
    }

    /**
     * Genera SyncQueueEntity para cada visita pendiente — usado por
     * SyncRepository.reEnqueueOrphans() para recuperar visitas que quedaron
     * con syncStatus='pending' pero sin entry en la cola (p.ej. tras un
     * crash o purga de la cola).
     */
    suspend fun getPendingOperations(): List<SyncQueueEntity> =
        visitDao.getPendingSync().map { visit ->
            SyncQueueEntity(
                entity    = "stop_visit",
                entityUid = visit.uid,
                operation = "upsert",
                payload   = mapAdapter.toJson(visitToMap(visit)),
            )
        }

    private suspend fun enqueue(visit: StopVisitEntity, op: String) {
        syncQueueDao.enqueue(SyncQueueEntity(
            entity    = "stop_visit",
            entityUid = visit.uid,
            operation = op,
            payload   = mapAdapter.toJson(visitToMap(visit)),
        ))
        triggerSync()
    }

    private fun triggerSync() {
        runCatching {
            WorkManager.getInstance(appContext)
                .enqueueUniqueWork(
                    SyncWorker.WORK_NAME_ONDEMAND,
                    ExistingWorkPolicy.REPLACE,
                    SyncWorker.onDemandRequest(),
                )
        }
    }

    /** Serialización para el batch_sync — claves snake_case que entiende api.php. */
    private fun visitToMap(v: StopVisitEntity): Map<String, Any?> = mapOf(
        "uid"           to v.uid,
        "stop_uid"      to v.stopUid,
        "route_uid"     to v.routeUid,
        "visit_date"    to v.visitDate,
        "status"        to v.status,
        "visited_at"    to v.visitedAt,
        "visit_result"  to v.visitResult,
        "next_action"   to v.nextAction,
        "notes"         to v.notes,
        "check_in_ts"   to v.checkInTs,
        "check_out_ts" to v.checkOutTs,
        "gps_lat_visit" to v.gpsLatVisit,
        "gps_lng_visit" to v.gpsLngVisit,
    )
}

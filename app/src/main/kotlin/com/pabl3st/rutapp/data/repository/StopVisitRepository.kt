package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.dao.StopVisitDao
import com.pabl3st.rutapp.data.local.entity.StopVisitEntity
import com.pabl3st.rutapp.data.session.SessionManager
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
    private val visitDao: StopVisitDao,
    private val session:  SessionManager,
) {
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private fun nowIso(): String = Instant.now().atOffset(ZoneOffset.UTC).format(isoFormatter)

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
        return updated
    }

    /** Inserta o reemplaza una visita venida del servidor (ya sincronizada). */
    suspend fun upsertFromServer(visit: StopVisitEntity) {
        visitDao.upsert(visit)
    }
}

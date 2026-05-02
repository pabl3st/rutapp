package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.core.location.LocationManager
import com.pabl3st.rutapp.data.local.dao.DaySessionDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.DaySessionEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.session.SessionManager
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JornadaRepository @Inject constructor(
    private val dao:          DaySessionDao,
    private val syncQueueDao: SyncQueueDao,
    private val session:      SessionManager,
    private val locationMgr:  LocationManager,
) {
    fun observe(routeUid: String, dateStr: String): Flow<DaySessionEntity?> =
        dao.observe(routeUid, dateStr)

    suspend fun get(routeUid: String, dateStr: String): DaySessionEntity? =
        dao.get(routeUid, dateStr)

    suspend fun start(routeUid: String, dateStr: String) {
        val now     = System.currentTimeMillis()
        val existing = dao.get(routeUid, dateStr)
        val session = existing?.copy(
            state     = "running",
            startedAt = existing.startedAt ?: now,
            pausedAt  = null,
            updatedAt = now,
        ) ?: DaySessionEntity(
            routeUid  = routeUid,
            dateStr   = dateStr,
            state     = "running",
            startedAt = now,
            updatedAt = now,
        )
        dao.upsert(session)
    }

    suspend fun pause(routeUid: String, dateStr: String) {
        val now     = System.currentTimeMillis()
        val session = dao.get(routeUid, dateStr) ?: return
        if (session.state != "running") return
        val elapsed = session.elapsedMs + (now - (session.pausedAt ?: session.startedAt ?: now))
        dao.updateState(routeUid, dateStr, "paused", now, elapsed, now)
    }

    suspend fun resume(routeUid: String, dateStr: String) {
        val session = dao.get(routeUid, dateStr) ?: return
        if (session.state != "paused") return
        val now = System.currentTimeMillis()
        dao.updateState(routeUid, dateStr, "running", null, session.elapsedMs, now)
    }

    suspend fun finish(routeUid: String, dateStr: String) {
        val now     = System.currentTimeMillis()
        val session = dao.get(routeUid, dateStr) ?: return
        val elapsed = when (session.state) {
            "running" -> session.elapsedMs + (now - (session.startedAt ?: now))
            else      -> session.elapsedMs
        }
        dao.updateState(routeUid, dateStr, "done", null, elapsed, now)
    }

    suspend fun updateGps(routeUid: String, dateStr: String, lat: Double, lng: Double) {
        val session = dao.get(routeUid, dateStr) ?: return
        if (session.state != "running") return
        val added = if (session.lastLat != null && session.lastLng != null) {
            locationMgr.distanceBetween(session.lastLat, session.lastLng, lat, lng) / 1000.0
        } else 0.0
        val newKm = session.distanceKm + added
        dao.updateDistance(routeUid, dateStr, newKm, lat, lng, System.currentTimeMillis())
    }

    // ── Sync al servidor ─────────────────────────────────────
    // Encola la sesión en sync_queue para que SyncWorker la suba
    // junto con el resto de datos pendientes en el siguiente ciclo.
    suspend fun enqueueSyncIfNeeded(routeUid: String, dateStr: String) {
        val session = dao.get(routeUid, dateStr) ?: return
        if (session.state == "idle") return  // no hay nada que sincronizar
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entity    = "day_session",
                entityUid = "${routeUid}_${dateStr}",
                operation = "upsert",
                payload   = buildJson(session),
            )
        )
    }

    private fun buildJson(s: DaySessionEntity): String {
        val startedAt = if (s.startedAt != null) s.startedAt.toString() else "null"
        return """{"routeUid":"${s.routeUid}","dateStr":"${s.dateStr}","state":"${s.state}","elapsedMs":${s.elapsedMs},"distanceKm":${s.distanceKm},"startedAt":$startedAt,"updatedAt":${s.updatedAt}}"""
    }

    // Tiempo transcurrido en ms (calculado en tiempo real sin leer BD)
    fun elapsedMs(session: DaySessionEntity): Long {
        return when (session.state) {
            "running" -> session.elapsedMs + (System.currentTimeMillis() - (session.startedAt ?: System.currentTimeMillis()))
            else      -> session.elapsedMs
        }
    }

    fun todayStr(): String = LocalDate.now().toString()
}

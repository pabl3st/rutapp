package com.pabl3st.rutapp.data.repository

import android.content.Context
import android.location.Location
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.pabl3st.rutapp.data.local.dao.DaySessionDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.DaySessionEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.sync.SyncWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JornadaRepository @Inject constructor(
    private val dao:          DaySessionDao,
    private val syncQueueDao: SyncQueueDao,
    private val moshi:        Moshi,
    @ApplicationContext private val appContext: Context,
) {
    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    fun observe(routeUid: String, dateStr: String): Flow<DaySessionEntity?> =
        dao.observe(routeUid, dateStr)

    suspend fun get(routeUid: String, dateStr: String): DaySessionEntity? =
        dao.get(routeUid, dateStr)

    suspend fun start(routeUid: String, dateStr: String) {
        val now      = System.currentTimeMillis()
        val existing = dao.get(routeUid, dateStr)
        val session  = existing?.copy(
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
        enqueueSession(session)
    }

    suspend fun pause(routeUid: String, dateStr: String) {
        val now     = System.currentTimeMillis()
        val session = dao.get(routeUid, dateStr) ?: return
        if (session.state != "running") return
        // Acumular tiempo desde el último startedAt (no desde pausedAt)
        val elapsed = session.elapsedMs + (now - (session.startedAt ?: now))
        dao.updateState(routeUid, dateStr, "paused",
            startedAt = null,  // limpiar startedAt al pausar
            pausedAt  = now,
            elapsedMs = elapsed,
            now       = now)
        dao.get(routeUid, dateStr)?.let { enqueueSession(it) }
    }

    suspend fun resume(routeUid: String, dateStr: String) {
        val session = dao.get(routeUid, dateStr) ?: return
        if (session.state != "paused") return
        val now = System.currentTimeMillis()
        // startedAt = now → el ticker mide desde este momento
        // pausedAt = null → limpiado para que pause() use startedAt correctamente
        dao.updateState(routeUid, dateStr, "running",
            startedAt = now,
            pausedAt  = null,
            elapsedMs = session.elapsedMs,
            now       = now)
        dao.get(routeUid, dateStr)?.let { enqueueSession(it) }
    }

    suspend fun finish(routeUid: String, dateStr: String) {
        val now     = System.currentTimeMillis()
        val session = dao.get(routeUid, dateStr) ?: return
        val elapsed = when (session.state) {
            "running" -> session.elapsedMs + (now - (session.startedAt ?: now))
            else      -> session.elapsedMs
        }
        dao.updateState(routeUid, dateStr, "done",
            startedAt = null,
            pausedAt  = null,
            elapsedMs = elapsed,
            now       = now)
        dao.get(routeUid, dateStr)?.let { enqueueSession(it) }
    }

    /** Reabre una jornada finalizada → vuelve a estado "running" */
    suspend fun reopen(routeUid: String, dateStr: String) {
        val session = dao.get(routeUid, dateStr) ?: return
        if (session.state != "done") return
        val now = System.currentTimeMillis()
        dao.updateState(routeUid, dateStr, "running",
            startedAt = now,
            pausedAt  = null,
            elapsedMs = session.elapsedMs,
            now       = now)
        dao.get(routeUid, dateStr)?.let { enqueueSession(it) }
    }

    suspend fun updateGps(routeUid: String, dateStr: String, lat: Double, lng: Double) {
        val session = dao.get(routeUid, dateStr) ?: return
        if (session.state != "running") return
        val added = if (session.lastLat != null && session.lastLng != null) {
            val results = FloatArray(1)
            Location.distanceBetween(session.lastLat, session.lastLng, lat, lng, results)
            results[0] / 1000.0
        } else 0.0
        val newKm = session.distanceKm + added
        dao.updateDistance(routeUid, dateStr, newKm, lat, lng, System.currentTimeMillis())
        // GPS updates son frecuentes — no encolamos cada uno para evitar flood en SyncQueue
        // Solo se encola en start/pause/resume/finish
    }

    // Tiempo transcurrido en ms (calculado en tiempo real sin leer BD)
    fun elapsedMs(session: DaySessionEntity): Long {
        return when (session.state) {
            "running" -> session.elapsedMs + (System.currentTimeMillis() - (session.startedAt ?: System.currentTimeMillis()))
            else      -> session.elapsedMs
        }
    }

    fun todayStr(): String = LocalDate.now().toString()

    // ── Encolar session en SyncQueue ──────────────────────────
    // api.php batch_sync entity="day_session": data con campos de la sesión
    private suspend fun enqueueSession(session: DaySessionEntity) {
        val payload = mapAdapter.toJson(
            mapOf(
                "routeUid"   to session.routeUid,
                "dateStr"    to session.dateStr,
                "state"      to session.state,
                "startedAt"  to session.startedAt,
                "elapsedMs"  to session.elapsedMs,
                "distanceKm" to session.distanceKm,
                "lastLat"    to session.lastLat,
                "lastLng"    to session.lastLng,
                "updatedAt"  to session.updatedAt,
            )
        )
        // Usar routeUid+dateStr como entityUid para que upserts sucesivos se sobreescriban en cola
        val entityUid = "${session.routeUid}|${session.dateStr}"
        // Si ya hay una entrada pendiente para esta sesión, eliminarla antes de reencolar
        // para evitar acumulación de duplicados (la cola es FIFO y puede crecer mucho)
        // Solución simple: el servidor usa ON DUPLICATE KEY UPDATE, así que duplicados no dañan
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entity    = "day_session",
                entityUid = entityUid,
                operation = "upsert",
                payload   = payload,
            )
        )
        // Sync inmediato: la jornada (play/pause/finish) sube ya si hay red.
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
}

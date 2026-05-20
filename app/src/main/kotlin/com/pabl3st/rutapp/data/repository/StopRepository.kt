package com.pabl3st.rutapp.data.repository
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.pabl3st.rutapp.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext

import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.session.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StopRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val stopDao:      StopDao,
    private val syncQueueDao: SyncQueueDao,
    private val session:      SessionManager,
    private val moshi:        Moshi,
) {
    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    fun observeByRoute(routeUid: String): Flow<List<StopEntity>> =
        stopDao.observeByRoute(routeUid)

    suspend fun getByRoute(routeUid: String): List<StopEntity> =
        stopDao.getByRoute(routeUid)

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

    /** Devuelve SyncQueueEntity para cada stop local pendiente de subir al servidor. */
    suspend fun getPendingOperations(): List<SyncQueueEntity> =
        stopDao.getPendingSync().map { stop ->
            SyncQueueEntity(
                entity    = "stop",
                entityUid = stop.uid,
                operation = "create",
                payload   = mapAdapter.toJson(stopToMap(stop)),
            )
        }

    suspend fun createStop(
        routeUid:       String,
        name:           String,
        externalId:     String? = null,
        address:        String? = null,
        lat:            Double? = null,
        lng:            Double? = null,
        orderIndex:     Int     = 0,
        notes:          String? = null,
        contactName:    String? = null,
        contactPhone:   String? = null,
        visitFrequency: Int?    = null,
        priority:       Int     = 3,
        segment:        String? = null,
    ): StopEntity {
        require(name.isNotBlank())     { "Nombre de parada vacío" }
        require(routeUid.isNotBlank()) { "routeUid vacío" }
        val now  = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val stop = StopEntity(
            uid            = UUID.randomUUID().toString(),
            routeUid       = routeUid,
            accountId      = session.accountId,
            name           = name,
            externalId     = externalId,
            address        = address,
            lat            = lat,
            lng            = lng,
            orderIndex     = orderIndex,
            notes          = notes,
            contactName    = contactName,
            contactPhone   = contactPhone,
            visitFrequency = visitFrequency,
            priority       = priority,
            segment        = segment,
            createdAt      = now,
            updatedAt      = now,
            syncStatus     = "pending",
        )
        stopDao.upsert(stop)
        enqueue("stop", stop.uid, "create", stopToMap(stop))
        triggerSync()
        return stop
    }

    /** Devuelve el historial de visitas a este PDV (hasta 20 más recientes).
     *  Si el stop tiene externalId, busca todas las instancias del mismo PDV.
     *  Si no, devuelve solo el stop actual si está done. */
    suspend fun getVisitHistory(stopUid: String): List<StopEntity> {
        val stop = stopDao.getByUid(stopUid) ?: return emptyList()
        return if (!stop.externalId.isNullOrBlank()) {
            stopDao.getVisitHistoryByExternalId(session.accountId, stop.externalId!!)
                .filter { it.uid != stopUid } // excluir el actual (ya se muestra en cabecera)
        } else {
            emptyList() // sin externalId no hay historial fiable
        }
    }

    suspend fun updateStop(
        uid:            String,
        name:           String,
        externalId:     String? = null,
        address:        String? = null,
        lat:            Double? = null,
        lng:            Double? = null,
        notes:          String? = null,
        contactName:    String? = null,
        contactPhone:   String? = null,
        visitFrequency: Int?    = null,
        priority:       Int     = 3,
        segment:        String? = null,
        openingHours:   String? = null,
    ) {
        val existing = stopDao.getByUid(uid) ?: return
        val now = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val updated = existing.copy(
            name           = name,
            externalId     = externalId,
            address        = address,
            lat            = lat ?: existing.lat,
            lng            = lng ?: existing.lng,
            notes          = notes,
            contactName    = contactName,
            contactPhone   = contactPhone,
            visitFrequency = visitFrequency,
            priority       = priority,
            segment        = segment,
            openingHours   = openingHours,
            updatedAt      = now,
            syncStatus     = "pending",
        )
        stopDao.upsert(updated)
        enqueue("stop", uid, "update", stopToMap(updated))
        triggerSync()
    }

    suspend fun markVisiting(uid: String) {
        stopDao.markVisiting(uid)
        // No enqueue — status visiting is transient, not synced until saveVisitResult
    }

    /** Resetea el stop a 'pending' borrando la visita anterior — usado por visitFrequency */
    suspend fun resetForNewVisit(uid: String) {
        val now = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        stopDao.resetForNewVisit(uid, now)
        val stop = stopDao.getByUid(uid) ?: return
        enqueue("stop", uid, "update", stopToMap(stop))
    }

    suspend fun markVisited(uid: String) {
        val now = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        stopDao.updateStatus(uid, "done", now, now)
        val stop = stopDao.getByUid(uid) ?: return
        enqueue("stop", uid, "update", stopToMap(stop))
    }

    suspend fun saveVisitResult(
        uid:         String,
        result:      String,
        notes:       String?,
        nextAction:  String?,
        pdvOpen:     Boolean  = true,
        pdvInactive: Boolean  = false,
        gpsLat:      Double?  = null,
        gpsLng:      Double?  = null,
    ) {
        val now = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        // accountStatus se actualiza automáticamente:
        // pdvInactive=true → "inactive"  |  pdvOpen=false → mantiene "active" (cerrado hoy)
        // Si vuelve a abrir tras inactivo → "active"
        val newAccountStatus = when {
            pdvInactive -> "inactive"
            pdvOpen     -> "active"
            else        -> null  // cerrado hoy — no cambiar accountStatus
        }
        val stopBefore = stopDao.getByUid(uid)
        val accountStatus = newAccountStatus ?: (stopBefore?.accountStatus ?: "active")
        stopDao.updateVisitResult(uid, result, notes, nextAction, pdvOpen, pdvInactive, accountStatus, now)
        // Actualizar GPS del check-in si se capturó
        if (gpsLat != null && gpsLng != null) {
            stopDao.updateCoords(uid, gpsLat, gpsLng, now)
        }
        val stopAfter = stopDao.getByUid(uid) ?: return
        enqueue("stop", uid, "update", stopToMap(stopAfter))
    }


    // ── Mapa global ───────────────────────────────────────────

    /** Todos los stops de las rutas dadas — para mapa global del día */
    fun observeByRouteUids(routeUids: List<String>): Flow<List<StopEntity>> =
        if (routeUids.isEmpty()) flowOf(emptyList())
        else stopDao.observeByRouteUids(routeUids)

    /** Solo los stops con GPS válido — para markers en mapa global */
    fun observeWithGpsByRouteUids(routeUids: List<String>): Flow<List<StopEntity>> =
        if (routeUids.isEmpty()) flowOf(emptyList())
        else stopDao.observeWithGpsByRouteUids(routeUids)

    suspend fun getByUid(uid: String): StopEntity? = stopDao.getByUid(uid)

    // ── Biblioteca de paradas ─────────────────────────────────

    fun observeAll(accountId: Int): Flow<List<StopEntity>> =
        stopDao.observeAll(accountId)

    fun observeWithoutGps(accountId: Int): Flow<List<StopEntity>> =
        stopDao.observeWithoutGps(accountId)

    fun observeOrphaned(accountId: Int): Flow<List<StopEntity>> =
        stopDao.observeOrphaned(accountId)

    // ── Reordenación de paradas ───────────────────────────────

    /** Persiste el orden actual de la lista en Room (bulk update) */
    @androidx.room.Transaction
    suspend fun reorderStops(stops: List<StopEntity>) {
        val now = java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        stops.forEachIndexed { index, stop ->
            stopDao.updateOrderIndex(stop.uid, index, now)
        }
    }

    /**
     * Geocodifica la dirección del stop y guarda las coordenadas en Room.
     * Llamar en background tras createStop si el stop tiene dirección.
     * No bloquea — si Nominatim falla, el stop queda sin coords (sin crash).
     */
    suspend fun geocodeAddress(uid: String, address: String, geocoder: suspend (String) -> com.pabl3st.rutapp.core.map.MapLatLng?) {
        val coords = geocoder(address) ?: return
        val now = java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        stopDao.updateCoords(uid, coords.lat, coords.lng, now)
        val stop = stopDao.getByUid(uid) ?: return
        enqueue("stop", uid, "update", stopToMap(stop))
    }

    private fun stopToMap(s: StopEntity): Map<String, Any?> = mapOf(
        "route_uid"    to s.routeUid,
        "name"         to s.name,
        "external_id"  to s.externalId,
        "address"      to s.address,
        "lat"          to s.lat,
        "lng"          to s.lng,
        "order_index"  to s.orderIndex,
        "status"       to s.status,
        "notes"        to s.notes,
        "contact_name"  to s.contactName,
        "contact_phone" to s.contactPhone,
        "visited_at"   to s.visitedAt,
        "visit_result" to s.visitResult,
        "next_action"  to s.nextAction,
        "pdv_open"     to if (s.pdvOpen) 1 else 0,
        "pdv_inactive"  to if (s.pdvInactive) 1 else 0,
        "created_at"      to s.createdAt,
        "visit_frequency" to s.visitFrequency,
        "priority"        to s.priority,
        "segment"         to s.segment,
        "account_status"  to s.accountStatus,
    )

    private suspend fun enqueue(entity: String, uid: String, op: String, data: Map<String, Any?>) {
        syncQueueDao.enqueue(SyncQueueEntity(
            entity    = entity,
            entityUid = uid,
            operation = op,
            payload   = mapAdapter.toJson(data),
        ))
    }
}




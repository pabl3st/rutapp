package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.session.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StopRepository @Inject constructor(
    private val stopDao:      StopDao,
    private val syncQueueDao: SyncQueueDao,
    private val session:      SessionManager,
    private val moshi:        Moshi,
) {
    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    fun observeByRoute(routeUid: String): Flow<List<StopEntity>> =
        stopDao.observeByRoute(routeUid)

    suspend fun createStop(
        routeUid:     String,
        name:         String,
        externalId:   String? = null,
        address:      String? = null,
        lat:          Double? = null,
        lng:          Double? = null,
        orderIndex:   Int     = 0,
        notes:        String? = null,
        contactName:  String? = null,
        contactPhone: String? = null,
    ): StopEntity {
        val now  = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val stop = StopEntity(
            uid          = UUID.randomUUID().toString(),
            routeUid     = routeUid,
            accountId    = session.accountId,
            name         = name,
            externalId   = externalId,
            address      = address,
            lat          = lat,
            lng          = lng,
            orderIndex   = orderIndex,
            notes        = notes,
            contactName  = contactName,
            contactPhone = contactPhone,
            createdAt    = now,
            updatedAt    = now,
            syncStatus   = "pending",
        )
        stopDao.upsert(stop)
        enqueue("stop", stop.uid, "create", stopToMap(stop))
        return stop
    }

    suspend fun markVisiting(uid: String) {
        stopDao.markVisiting(uid)
        // No enqueue — status visiting is transient, not synced until saveVisitResult
    }

    suspend fun markVisited(uid: String) {
        val now = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        stopDao.updateStatus(uid, "done", now, now)
        val stop = stopDao.getByUid(uid) ?: return
        enqueue("stop", uid, "update", stopToMap(stop))
    }

    suspend fun saveVisitResult(uid: String, result: String, notes: String?, nextAction: String?) {
        val now = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        stopDao.updateVisitResult(uid, result, notes, nextAction, now)
        val stop = stopDao.getByUid(uid) ?: return
        enqueue("stop", uid, "update", stopToMap(stop))
    }

    suspend fun getByUid(uid: String): StopEntity? = stopDao.getByUid(uid)

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
        "created_at"   to s.createdAt,
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

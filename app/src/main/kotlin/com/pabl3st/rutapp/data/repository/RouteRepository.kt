package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.dao.DaySessionDao
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.network.RouteDto
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val routeDao:      RouteDao,
    private val stopDao:       StopDao,
    private val daySessionDao: DaySessionDao,
    private val kpiValueDao:   KpiValueDao,
    private val syncQueueDao:  SyncQueueDao,
    private val api:           RutasApiService,
    private val session:       SessionManager,
    private val moshi:         Moshi,
) {
    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    // ── Roles con visibilidad ampliada ────────────────────────
    private val isManager: Boolean
        get() = session.userRole in listOf("owner", "admin", "manager", "god")

    fun observeToday(): Flow<List<RouteEntity>> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return if (isManager)
            routeDao.observeByDateForAccount(session.accountId, today)
        else
            routeDao.observeByDate(session.userId, today)
    }

    fun observeAll(): Flow<List<RouteEntity>> =
        if (isManager) routeDao.observeByAccount(session.accountId)
        else           routeDao.observeByUser(session.userId)

    suspend fun getByUid(uid: String): RouteEntity? =
        routeDao.getByUid(uid)

    /** Desasigna la ruta del calendario — fecha se pone a 1970-01-01 */
    suspend fun unassignDate(uid: String) {
        val route = routeDao.getByUid(uid) ?: return
        val now   = java.time.Instant.now().toString()
        val updated = route.copy(
            dateAssigned = "1970-01-01",
            status       = "pending",
            updatedAt    = now,
            syncStatus   = "pending",
        )
        routeDao.upsert(updated)
        enqueue("route", uid, "update", routeToMap(updated))
    }

    /** Asigna la ruta a una fecha concreta del calendario */
    suspend fun assignDate(uid: String, dateStr: String) {
        val route = routeDao.getByUid(uid) ?: return
        val now   = java.time.Instant.now().toString()
        val updated = route.copy(
            dateAssigned = dateStr,
            updatedAt    = now,
            syncStatus   = "pending",
        )
        routeDao.upsert(updated)
        enqueue("route", uid, "update", routeToMap(updated))
    }

    // ── Crear ruta localmente + encolar sync ──────────────────
    suspend fun createRoute(
        name: String,
        dateAssigned: String,
        notes: String? = null,
    ): RouteEntity {
        val now   = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val route = RouteEntity(
            uid          = UUID.randomUUID().toString(),
            accountId    = session.accountId,
            userId       = session.userId,
            name         = name,
            dateAssigned = dateAssigned,
            notes        = notes,
            createdAt    = now,
            updatedAt    = now,
            syncStatus   = "pending",
        )
        routeDao.upsert(route)
        enqueue("route", route.uid, "create", routeToMap(route))
        return route
    }

    // ── Delta sync desde servidor ──────────────────────────────
    private val lastFetchMs = AtomicLong(0L)

    suspend fun fetchDelta(): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        if (now - lastFetchMs.get() < 10_000L) return@runCatching
        lastFetchMs.set(now)
        val token = session.token ?: return@runCatching
        val since = session.lastSyncTimestamp.ifEmpty { "2000-01-01T00:00:00Z" }
        val resp  = api.deltaSync(token = token, since = since)
        if (!resp.isSuccessful || resp.body()?.ok != true) return@runCatching

        val body = resp.body()!!
        body.routes?.map { it.toEntity(session.userId, session.accountId) }
            ?.let { routeDao.upsertAll(it) }
        body.stops?.mapNotNull { it.toEntity(session.accountId) }
            ?.let { if (it.isNotEmpty()) stopDao.upsertAll(it) }
        body.daySessions?.mapNotNull { toEntity(it) }
            ?.let { if (it.isNotEmpty()) it.forEach { s -> daySessionDao.upsert(s) } }
        body.kpiValues?.mapNotNull { toKpiEntity(it) }
            ?.let { if (it.isNotEmpty()) kpiValueDao.upsertAll(it) }
        body.serverTime?.let { session.lastSyncTimestamp = it }
    }

    private fun toEntity(dto: com.pabl3st.rutapp.data.network.DaySessionDto) =
        com.pabl3st.rutapp.data.local.entity.DaySessionEntity(
            routeUid   = dto.routeUid,
            dateStr    = dto.dateStr,
            state      = dto.state,
            startedAt  = dto.startedAt,
            elapsedMs  = dto.elapsedMs,
            distanceKm = dto.distanceKm,
            lastLat    = dto.lastLat,
            lastLng    = dto.lastLng,
            updatedAt  = dto.updatedAt,
        )

    private fun toKpiEntity(dto: com.pabl3st.rutapp.data.network.KpiValueDto): com.pabl3st.rutapp.data.local.entity.KpiValueEntity? {
        if (dto.stopUid.isBlank() || dto.kpiId.isBlank()) return null
        return com.pabl3st.rutapp.data.local.entity.KpiValueEntity(
            stopUid    = dto.stopUid,
            kpiId      = dto.kpiId,
            valueText  = dto.valueText ?: "",
            syncStatus = "synced",
        )
    }

    // ── Helpers ───────────────────────────────────────────────
    private fun routeToMap(r: RouteEntity): Map<String, Any?> = mapOf(
        "name"          to r.name,
        "date_assigned" to r.dateAssigned,
        "status"        to r.status,
        "notes"         to r.notes,
        "created_at"    to r.createdAt,
    )

    private suspend fun enqueue(entity: String, uid: String, op: String, data: Map<String, Any?>) {
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entity    = entity,
                entityUid = uid,
                operation = op,
                payload   = mapAdapter.toJson(data),
            )
        )
    }
}

package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.network.RouteDto
import com.pabl3st.rutapp.data.network.toEntity
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val routeDao:     RouteDao,
    private val stopDao:      StopDao,
    private val syncQueueDao: SyncQueueDao,
    private val api:          RutasApiService,
    private val session:      SessionManager,
    private val moshi:        Moshi,
) {
    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    // ── Observar rutas del día — reactivo desde Room ──────────
    fun observeToday(): Flow<List<RouteEntity>> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return routeDao.observeByDate(session.userId, today)
    }

    fun observeAll(): Flow<List<RouteEntity>> =
        routeDao.observeByUser(session.userId)

    suspend fun getByUid(uid: String): RouteEntity? =
        routeDao.getByUid(uid)

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
    suspend fun fetchDelta(): Result<Unit> = runCatching {
        val token = session.token ?: return@runCatching
        val since = session.lastSyncTimestamp.ifEmpty { "2000-01-01T00:00:00Z" }
        val resp  = api.deltaSync(token = token, since = since)
        if (!resp.isSuccessful || resp.body()?.ok != true) return@runCatching

        val body = resp.body()!!
        body.routes?.map { it.toEntity(session.userId, session.accountId) }
            ?.let { routeDao.upsertAll(it) }
        body.stops?.mapNotNull { it.toEntity(session.accountId) }
            ?.let { if (it.isNotEmpty()) stopDao.upsertAll(it) }
        body.serverTime?.let { session.lastSyncTimestamp = it }
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

// ── Extension: RouteDto → RouteEntity ────────────────────────
fun RouteDto.toEntity(userId: Int, accountId: Int) = RouteEntity(
    uid          = uid,
    serverId     = id,
    accountId    = accountId,
    userId       = userId,
    name         = name,
    dateAssigned = dateAssigned,
    status       = status,
    notes        = notes,
    createdAt    = createdAt,
    updatedAt    = updatedAt,
    deletedAt    = deletedAt,
    syncStatus   = "synced",
    syncedAt     = updatedAt,
)



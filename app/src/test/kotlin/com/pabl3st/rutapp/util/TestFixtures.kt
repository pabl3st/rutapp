package com.pabl3st.rutapp.util

import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.network.AccountDto
import com.pabl3st.rutapp.data.network.AuthResponse
import com.pabl3st.rutapp.data.network.BatchSyncResponse
import com.pabl3st.rutapp.data.network.BatchSyncResult
import com.pabl3st.rutapp.data.network.DeltaSyncResponse
import com.pabl3st.rutapp.data.network.RouteDto
import com.pabl3st.rutapp.data.network.StopDto
import com.pabl3st.rutapp.data.network.UserDto

/**
 * Fuente única de datos de prueba.
 * Añadir aquí cuando se añadan nuevas entidades al proyecto.
 * Todos los tests importan desde aquí — nunca crear datos inline en los tests.
 */
object TestFixtures {

    // ── IDs y constantes ─────────────────────────────────────
    const val USER_ID    = 2
    const val ACCOUNT_ID = 2
    const val TOKEN      = "test_token_64chars_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    const val SERVER_TIME = "2026-05-01T19:00:00+02:00"

    // ── RouteDto (servidor → app) ─────────────────────────────
    fun routeDto(
        id: Int = 1,
        uid: String = "route-uid-001",
        name: String = "Ruta Centro Valencia",
        dateAssigned: String = "2026-05-01",
        status: String = "pending",
        notes: String? = "Test notes",
        createdAt: String = "2026-05-01 10:00:00",
        updatedAt: String = "2026-05-01 10:00:00",
        deletedAt: String? = null,
    ) = RouteDto(
        id           = id,
        uid          = uid,
        name         = name,
        dateAssigned = dateAssigned,
        status       = status,
        notes        = notes,
        stopCount    = 0,
        doneCount    = 0,
        createdAt    = createdAt,
        updatedAt    = updatedAt,
        deletedAt    = deletedAt,
    )

    // ── StopDto (servidor → app) ──────────────────────────────
    fun stopDto(
        id: Int = 1,
        uid: String = "stop-uid-001",
        routeUid: String? = "route-uid-001",
        name: String = "Distribuciones Martínez",
        address: String? = "Calle Colón 12, Valencia",
        lat: Double? = 39.4699,
        lng: Double? = -0.3763,
        orderIndex: Int = 0,
        status: String = "pending",
        notes: String? = null,
        visitedAt: String? = null,
        createdAt: String = "2026-05-01 10:00:00",
        updatedAt: String = "2026-05-01 10:00:00",
    ) = StopDto(
        id         = id,
        uid        = uid,
        routeUid   = routeUid,
        name       = name,
        address    = address,
        lat        = lat,
        lng        = lng,
        orderIndex = orderIndex,
        status     = status,
        notes      = notes,
        visitedAt  = visitedAt,
        createdAt  = createdAt,
        updatedAt  = updatedAt,
        deletedAt  = null,
    )

    // ── RouteEntity (Room) ────────────────────────────────────
    fun routeEntity(
        uid: String = "route-uid-001",
        serverId: Int? = 1,
        accountId: Int = ACCOUNT_ID,
        userId: Int = USER_ID,
        name: String = "Ruta Centro Valencia",
        dateAssigned: String = "2026-05-01",
        status: String = "pending",
        notes: String? = "Test notes",
        syncStatus: String = "synced",
        createdAt: String = "2026-05-01T10:00:00Z",
        updatedAt: String = "2026-05-01T10:00:00Z",
    ) = RouteEntity(
        uid          = uid,
        serverId     = serverId,
        accountId    = accountId,
        userId       = userId,
        name         = name,
        dateAssigned = dateAssigned,
        status       = status,
        notes        = notes,
        syncStatus   = syncStatus,
        createdAt    = createdAt,
        updatedAt    = updatedAt,
    )

    // ── StopEntity (Room) ─────────────────────────────────────
    fun stopEntity(
        uid: String = "stop-uid-001",
        serverId: Int? = 1,
        routeUid: String = "route-uid-001",
        accountId: Int = ACCOUNT_ID,
        name: String = "Distribuciones Martínez",
        address: String? = "Calle Colón 12, Valencia",
        lat: Double? = 39.4699,
        lng: Double? = -0.3763,
        orderIndex: Int = 0,
        status: String = "pending",
        syncStatus: String = "synced",
        createdAt: String = "2026-05-01T10:00:00Z",
        updatedAt: String = "2026-05-01T10:00:00Z",
    ) = StopEntity(
        uid        = uid,
        serverId   = serverId,
        routeUid   = routeUid,
        accountId  = accountId,
        name       = name,
        address    = address,
        lat        = lat,
        lng        = lng,
        orderIndex = orderIndex,
        status     = status,
        createdAt  = createdAt,
        updatedAt  = updatedAt,
        syncStatus = syncStatus,
    )

    // ── SyncQueueEntity ───────────────────────────────────────
    fun syncQueueEntity(
        id: Int = 1,
        entity: String = "route",
        entityUid: String = "route-uid-001",
        operation: String = "create",
        payload: String = """{"name":"Test","date_assigned":"2026-05-01","status":"pending"}""",
        attempts: Int = 0,
    ) = SyncQueueEntity(
        id        = id,
        entity    = entity,
        entityUid = entityUid,
        operation = operation,
        payload   = payload,
        attempts  = attempts,
    )

    // ── DeltaSyncResponse ─────────────────────────────────────
    fun deltaSyncResponse(
        routes: List<RouteDto> = listOf(routeDto()),
        stops:  List<StopDto>  = listOf(stopDto()),
        serverTime: String     = SERVER_TIME,
    ) = DeltaSyncResponse(
        ok         = true,
        routes     = routes,
        stops      = stops,
        serverTime = serverTime,
        error      = null,
    )

    // ── BatchSyncResponse ─────────────────────────────────────
    fun batchSyncResponse(
        syncedUids: List<String> = listOf("route-uid-001"),
        errorUids:  List<String> = emptyList(),
        entity: String = "route",
    ) = BatchSyncResponse(
        ok     = true,
        synced = syncedUids.map { BatchSyncResult(uid = it, entity = entity, serverId = 1) },
        errors = errorUids.map  { BatchSyncResult(uid = it, entity = entity, error = "Error") },
        serverTime = SERVER_TIME,
        error  = null,
    )

    // ── AuthResponse ──────────────────────────────────────────
    fun authResponse(
        token: String = TOKEN,
        userId: Int = USER_ID,
        username: String = "god",
        email: String = "god@rutasapp.dev",
        role: String = "owner",
        accountId: Int = ACCOUNT_ID,
        accountType: String = "individual",
    ) = AuthResponse(
        ok            = true,
        token         = token,
        expiresInDays = 30,
        user          = UserDto(
            id        = userId,
            username  = username,
            email     = email,
            name      = "God Admin",
            role      = role,
            avatarUrl = null,
            accountId = accountId,
            createdAt = "2026-05-01 08:00:00",
        ),
        account       = AccountDto(
            id         = accountId,
            type       = accountType,
            name       = "God Admin",
            slug       = "god-admin",
            plan       = "free",
            plusConfig = null,
            formConfig = null,
            aiSettings = null,
        ),
        prefs         = emptyMap(),
        error         = null,
    )
}

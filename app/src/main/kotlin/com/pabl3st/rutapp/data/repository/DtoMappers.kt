package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.network.RouteDto
import com.pabl3st.rutapp.data.network.StopDto

// ── RouteDto → RouteEntity ────────────────────────────────────
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

// ── StopDto → StopEntity ──────────────────────────────────────
fun StopDto.toEntity(accountId: Int): StopEntity? {
    val rUid = routeUid ?: return null   // sin route_uid no podemos linkarlo a Room
    return StopEntity(
        uid        = uid,
        serverId   = id,
        routeUid   = rUid,
        accountId  = accountId,
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
        deletedAt  = deletedAt,
        syncStatus = "synced",
        syncedAt   = updatedAt,
    )
}

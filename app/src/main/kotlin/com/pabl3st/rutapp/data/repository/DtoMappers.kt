package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.StopEntity
import com.pabl3st.rutapp.data.network.RouteDto
import com.pabl3st.rutapp.data.network.StopDto

// ── RouteDto → RouteEntity ────────────────────────────────────
fun RouteDto.toEntity(userId: Int, accountId: Int) = RouteEntity(
    uid             = uid,
    serverId        = id,
    accountId       = accountId,
    userId          = if (this.userId != 0) this.userId else userId,  // preservar userId del servidor
    name            = name,
    dateAssigned    = dateAssigned,
    scheduledDates  = scheduledDates?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() },
    status          = status,
    notes           = notes,
    createdAt       = createdAt,
    updatedAt       = updatedAt,
    deletedAt       = deletedAt,
    syncStatus      = "synced",
    syncedAt        = updatedAt,
)

// ── StopDto → StopEntity ──────────────────────────────────────
fun StopDto.toEntity(accountId: Int): StopEntity? {
    val rUid = routeUid ?: return null   // sin route_uid no podemos linkarlo a Room
    return StopEntity(
        uid          = uid,
        serverId     = id,
        routeUid     = rUid,
        accountId    = accountId,
        name         = name,
        externalId   = externalId,
        address      = address,
        lat          = lat,
        lng          = lng,
        orderIndex   = orderIndex,
        status       = status,
        notes        = notes,
        contactName  = contactName,
        contactPhone = contactPhone,
        visitedAt    = visitedAt,
        visitResult  = visitResult,
        nextAction   = nextAction,
        pdvOpen        = pdvOpen,
        pdvInactive    = pdvInactive,
        visitFrequency = visitFrequency?.toIntOrNull(),   // StopDto:String? → StopEntity:Int?
        priority       = priority ?: 0,                   // StopDto:Int?   → StopEntity:Int
        segment        = segment,
        accountStatus  = accountStatus ?: "active",       // StopDto:String? → StopEntity:String
        createdAt      = createdAt,
        updatedAt    = updatedAt,
        deletedAt    = deletedAt,
        dateAssigned = dateAssigned,
        checkInTs    = checkInTs,
        checkOutTs   = checkOutTs,
        gpsLatVisit  = gpsLatVisit,
        gpsLngVisit  = gpsLngVisit,
        syncStatus   = "synced",
        syncedAt     = updatedAt,
    )
}

fun com.pabl3st.rutapp.data.network.BusinessProfileSyncDto.toEntity(accountId: Int) =
    com.pabl3st.rutapp.data.local.entity.BusinessProfileEntity(
        accountId   = accountId,
        sector      = sector,
        name        = name,
        updatedAt   = System.currentTimeMillis(),
    )

fun com.pabl3st.rutapp.data.network.KpiDefinitionSyncDto.toEntity() =
    com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity(
        id          = id,
        accountId   = accountId,
        sector      = sector,
        label       = label,
        type        = type,
        unit        = unit,
        options     = options,
        isSystem    = isSystem == 1,
        visible     = visible == 1,
        required    = required == 1,
        orderIndex  = orderIndex,
        section     = section,
    )


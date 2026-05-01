package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey val uid: String,
    val serverId: Int?      = null,
    val routeUid: String,                  // FK lógica a RouteEntity.uid
    val accountId: Int,
    val name: String,
    val address: String?    = null,
    val lat: Double?        = null,
    val lng: Double?        = null,
    val orderIndex: Int     = 0,
    val status: String      = "pending",   // pending|visiting|done|skipped
    val notes: String?      = null,
    val visitedAt: String?  = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?  = null,
    val syncStatus: String  = "pending",
    val syncedAt: String?   = null,
)

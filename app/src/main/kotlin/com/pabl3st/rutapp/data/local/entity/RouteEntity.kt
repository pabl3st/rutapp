package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val uid: String,           // UUID generado en cliente
    val serverId: Int?     = null,         // id del servidor tras sync
    val accountId: Int,
    val userId: Int,
    val name: String,
    val dateAssigned: String,              // ISO8601 "YYYY-MM-DD"
    val status: String     = "pending",    // pending|active|done|cancelled
    val notes: String?     = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = "pending",    // pending|synced|error
    val syncedAt: String?  = null,
    /** JSON array de fechas de visita programadas: ["2026-05-12","2026-05-21"] */
    val scheduledDates: String? = null,
)


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
    val dateAssigned: String,              // ISO8601 "YYYY-MM-DD" — primera ejecución
    val status: String     = "pending",    // pending|active|done|cancelled
    val notes: String?     = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = "pending",    // pending|synced|error
    val syncedAt: String?  = null,
    /** Nº de paradas de la ruta — calculado por el servidor, llega en routes_list. 0 si aún no sincronizada. */
    val stopCount: Int     = 0,
    /** Nº de paradas completadas — calculado por el servidor. */
    val doneCount: Int     = 0,
    /** Fechas de visita programadas — convertido automáticamente a/desde JSON por RutasTypeConverters */
    val scheduledDates: List<String>? = null,
)


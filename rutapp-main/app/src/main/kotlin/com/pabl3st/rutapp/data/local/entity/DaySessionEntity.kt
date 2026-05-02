package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName  = "day_sessions",
    primaryKeys = ["routeUid", "dateStr"],
    indices    = [Index("dateStr")],
)
data class DaySessionEntity(
    val routeUid:   String,
    val dateStr:    String,             // YYYY-MM-DD
    val state:      String = "idle",    // idle|running|paused|done
    val startedAt:  Long?  = null,      // epoch ms — primer play
    val pausedAt:   Long?  = null,      // epoch ms — último pause
    val elapsedMs:  Long   = 0L,        // ms acumulados (sin contar pausa actual)
    val distanceKm: Double = 0.0,       // km recorridos (Haversine acumulado)
    val lastLat:    Double? = null,     // última posición conocida
    val lastLng:    Double? = null,
    val updatedAt:  Long   = System.currentTimeMillis(),
)

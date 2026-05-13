package com.pabl3st.rutapp.data.local

import androidx.room.TypeConverter

/**
 * TypeConverters para Room.
 * - scheduledDates: List<String> ↔ String? (JSON array)
 * - SyncStatus: String ("pending"|"synced"|"error") — sin conversión extra,
 *   pero centraliza la lógica de valores válidos.
 */
class RutasTypeConverters {

    // ── scheduledDates: ["2026-05-12","2026-05-21"] ↔ JSON string ──────────
    @TypeConverter
    fun scheduledDatesToString(dates: List<String>?): String? {
        if (dates.isNullOrEmpty()) return null
        return dates.joinToString(",") { it.trim() }
    }

    @TypeConverter
    fun stringToScheduledDates(value: String?): List<String>? {
        if (value.isNullOrBlank()) return null
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

// ── SyncStatus — valores válidos centralizados ───────────────────────────────
// No necesita TypeConverter (Room ya maneja String), pero centraliza los literales
// para evitar strings mágicos dispersos por el código.
object SyncStatus {
    const val PENDING = "pending"
    const val SYNCED  = "synced"
    const val ERROR   = "error"
}

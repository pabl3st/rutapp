package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey val uid: String,           // UUID interno — clave técnica, nunca visible
    val serverId: Int?      = null,
    val routeUid: String,
    val accountId: Int,

    // ── Identificación ───────────────────────────────────────
    val name: String,
    val externalId: String? = null,        // id del cliente (LCC00237, PREP011085…)
                                            // usado en import/export bulk con sus sistemas

    // ── Ubicación ────────────────────────────────────────────
    val address: String?    = null,
    val lat: Double?        = null,
    val lng: Double?        = null,
    val orderIndex: Int     = 0,

    // ── Contacto (S06) ───────────────────────────────────────
    val contactName: String?  = null,      // persona de contacto
    val contactPhone: String? = null,      // teléfono directo

    // ── Estado y visita ───────────────────────────────────────
    val status: String      = "pending",   // pending|visiting|done|skipped
    val notes: String?      = null,
    val visitedAt: String?  = null,

    // ── Resultado visita (S06) ────────────────────────────────
    val visitResult: String?  = null,      // contactado|no_estaba|volvemos|rechazado
    val nextAction: String?   = null,      // texto libre para próxima visita

    // ── Sync ─────────────────────────────────────────────────
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?  = null,
    val syncStatus: String  = "pending",
    val syncedAt: String?   = null,
)

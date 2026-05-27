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
    val address: String?    = null,        // dirección completa (compat + display)
    val street: String?     = null,        // calle + número (separado de address)
    val postalCode: String? = null,        // CP (5 dígitos en España, alfanumérico permitido)
    val city: String?       = null,        // localidad
    val lat: Double?        = null,
    val lng: Double?        = null,
    val orderIndex: Int     = 0,

    // ── Contacto (S06) ───────────────────────────────────────
    val contactName: String?  = null,      // persona de contacto
    val contactPhone: String? = null,      // teléfono directo

    // ── Campos universales (S05) ─────────────────────────────
    val visitFrequency: Int?    = null,    // días entre visitas (7, 14, 30...)
    val priority: Int           = 0,       // 0=sin asignar, 1=máxima, 5=mínima
    val segment: String?        = null,    // A/B/C o tier personalizado
    val accountStatus: String   = "active",// prospect|active|inactive|churned
    val openingHours: String?   = null,    // JSON {"mon":"9-18","tue":"9-18"...}
    val pdvOpen: Boolean        = true,         // PDV abierto en la última visita (cerrado hoy)
    val pdvInactive: Boolean    = false,        // PDV cerrado permanentemente (inactivo)

    // ── Estado y visita ───────────────────────────────────────
    val dateAssigned: String? = null,      // fecha de esta visita concreta (YYYY-MM-DD)
    val status: String      = "pending",   // pending|visiting|done|skipped
    val notes: String?      = null,
    val visitedAt:    String?  = null,
    val checkInTs:    Long?    = null,   // epoch ms — cuando se abrió el formulario
    val checkOutTs:   Long?    = null,   // epoch ms — cuando se guardó la visita
    val gpsLatVisit:  Double?  = null,   // GPS del agente al momento del check-in
    val gpsLngVisit:  Double?  = null,

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



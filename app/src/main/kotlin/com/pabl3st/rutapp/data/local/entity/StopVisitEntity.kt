package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Una visita programada o realizada a un stop en una fecha concreta.
 * PK: uid (UUID generado en cliente).
 * UNIQUE: (stopUid, visitDate) — evita duplicados al sincronizar.
 *
 * Cada fecha programada de una ruta crea una stop_visit "pending".
 * El agente la pasa a "visiting" al check-in y a "done"/"skipped" al guardar.
 * Los KPIs y fotos de esa visita se enlazan vía visitUid.
 *
 * El stop (StopEntity) conserva campos espejo de la última visita
 * (status, visitedAt, visitResult…) para compatibilidad con vistas
 * legacy (Biblioteca, lista de PDVs) sin necesidad de hacer JOIN.
 */
@Entity(
    tableName = "stop_visits",
    indices = [
        Index(value = ["stopUid", "visitDate"], unique = true),
        Index("routeUid"),
        Index("syncStatus"),
    ],
)
data class StopVisitEntity(
    @PrimaryKey val uid: String,             // UUID del cliente
    val stopUid:   String,                   // FK lógica → stops.uid
    val routeUid:  String,                   // FK lógica → routes.uid (denormalizado para queries por ruta)
    val accountId: Int,
    val visitDate: String,                   // YYYY-MM-DD — fecha programada de la visita

    // ── Estado y resultado ──────────────────────────────────────
    val status:      String  = "pending",    // pending|visiting|done|skipped
    val visitedAt:   String? = null,         // ISO8601 — momento de finalización
    val visitResult: String? = null,         // contactado|no_estaba|volvemos|rechazado
    val nextAction:  String? = null,         // texto libre próxima visita
    val notes:       String? = null,

    // ── Telemetría ──────────────────────────────────────────────
    val checkInTs:    Long?   = null,        // epoch ms — apertura del formulario
    val checkOutTs:   Long?   = null,        // epoch ms — guardado
    val gpsLatVisit:  Double? = null,
    val gpsLngVisit:  Double? = null,

    // ── Auditoría + sync ────────────────────────────────────────
    val createdAt:  String,
    val updatedAt:  String,
    val deletedAt:  String? = null,
    val syncStatus: String   = "pending",    // pending|synced|error
    val syncedAt:   String?  = null,
)

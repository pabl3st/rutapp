package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Valor de un KPI para una visita concreta.
 *
 * Modelo C (a partir de Room v17): PK = (visitUid, kpiId) — un valor por
 * KPI por VISITA. Si un PDV se visita 4 veces al mes, tiene 4 sets de KPIs.
 *
 * `stopUid` se conserva como campo NO-PK para:
 * - queries históricas por PDV (`getByStop`)
 * - vistas legacy (Biblioteca, GlobalMap)
 * Sincroniza bidireccional con servidor (tabla kpi_values, columna visit_uid).
 */
@Entity(
    tableName = "kpi_values",
    primaryKeys = ["visitUid", "kpiId"],
    indices = [
        Index("stopUid"),
        Index("syncStatus"),
    ],
)
data class KpiValueEntity(
    val visitUid:  String,    // FK lógica → stop_visits.uid (PK nueva)
    val stopUid:   String,    // FK lógica → stops.uid (para queries por PDV)
    val kpiId:     String,    // e.g. "telco_activaciones"
    val valueText: String,    // valor serializado como string
    val syncStatus: String = "pending",  // pending|synced|error
)

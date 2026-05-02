package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity

/**
 * Valor de un KPI para una visita concreta.
 * PK compuesta: stopUid + kpiId — un valor por KPI por stop.
 * Sincroniza bidireccional con servidor (tabla kpi_values).
 */
@Entity(
    tableName = "kpi_values",
    primaryKeys = ["stopUid", "kpiId"],
)
data class KpiValueEntity(
    val stopUid:   String,    // FK → stops.uid
    val kpiId:     String,    // e.g. "telco_activaciones"
    val valueText: String,    // valor serializado como string
    val syncStatus: String = "pending",  // pending|synced|error
)

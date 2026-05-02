package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Define un KPI medible en una visita.
 * El accountId = 0 indica que es un KPI de sistema (predefinido por sector).
 * El accountId > 0 indica que es un KPI custom del account.
 */
@Entity(tableName = "kpi_definitions")
data class KpiDefinitionEntity(
    @PrimaryKey val id: String,            // e.g. "telco_activaciones", "custom_abc123"
    val accountId: Int,                    // 0 = sistema/predefinido, >0 = custom del account
    val sector: String,                    // telco|farma|distribucion|retail|common|custom
    val label: String,                     // "Activaciones", "Unidades vendidas", etc.
    val type: String,                      // number|boolean|select|text
    val unit: String?      = null,         // "€", "ud", "%", null = sin unidad
    val options: String?   = null,         // JSON array para type=select: ["Sí","No","Parcial"]
    val required: Boolean  = false,
    val visible: Boolean   = true,
    val orderIndex: Int    = 0,
    val section: String    = "general",    // sección del formulario: general|objetivos|pedidos|notas
    val isSystem: Boolean  = true,         // false = creado por el usuario
)

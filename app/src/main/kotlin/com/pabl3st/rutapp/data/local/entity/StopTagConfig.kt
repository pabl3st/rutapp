package com.pabl3st.rutapp.data.local.entity

import androidx.compose.ui.graphics.Color

/**
 * Configuración de un tag personalizado que aparece en las cards de paradas.
 *
 * El owner define nombre, icono, color y condición de aparición.
 * Se persiste en DataStore y se serializa a JSON para sincronizar al servidor.
 *
 * Condiciones soportadas:
 *   - ALWAYS           → siempre visible
 *   - STATUS_DONE      → cuando el stop está visitado
 *   - STATUS_PENDING   → cuando el stop está pendiente
 *   - PDV_OPEN         → cuando pdvOpen = true (última visita PDV abierto)
 *   - PDV_CLOSED       → cuando pdvOpen = false
 *   - RESULT_IS        → cuando visitResult == conditionValue
 *   - KPI_ABOVE        → cuando el KPI de id conditionKpiId tiene valor > conditionThreshold
 *   - KPI_BELOW        → cuando el KPI de id conditionKpiId tiene valor < conditionThreshold
 *   - KPI_BOOL_TRUE    → cuando el KPI boolean de id conditionKpiId es "true"
 *   - DAYS_SINCE_VISIT → cuando han pasado ≥ conditionThreshold días desde la última visita
 */
data class StopTagConfig(
    val id:               String,          // UUID local
    val name:             String,          // texto del tag (ej: "ACTIVO PLUS")
    val icon:             String,          // nombre de MaterialIcon (ej: "Star", "Warning")
    val colorHex:         String,          // color de fondo en hex (ej: "#dcfce7")
    val textColorHex:     String,          // color de texto en hex (ej: "#15803d")
    val condition:        TagCondition,
    val conditionValue:   String?  = null, // para RESULT_IS: valor del resultado
    val conditionKpiId:   String?  = null, // para KPI_ABOVE/KPI_BELOW/KPI_BOOL_TRUE
    val conditionThreshold: Double = 0.0,  // para KPI_ABOVE/KPI_BELOW/DAYS_SINCE_VISIT
    val enabled:          Boolean  = true,
)

enum class TagCondition {
    ALWAYS,
    STATUS_DONE,
    STATUS_PENDING,
    PDV_OPEN,
    PDV_CLOSED,
    RESULT_IS,
    KPI_ABOVE,
    KPI_BELOW,
    KPI_BOOL_TRUE,
    DAYS_SINCE_VISIT,
}

/** Etiquetas predefinidas del sistema — no editables, siempre presentes */
val DEFAULT_SYSTEM_TAGS = listOf(
    StopTagConfig(
        id           = "sys_visitado",
        name         = "Visitado",
        icon         = "CheckCircle",
        colorHex     = "#dcfce7",
        textColorHex = "#15803d",
        condition    = TagCondition.STATUS_DONE,
    ),
    StopTagConfig(
        id           = "sys_pdv_cerrado",
        name         = "Cerrado",
        icon         = "StoreMallDirectory",
        colorHex     = "#fee2e2",
        textColorHex = "#dc2626",
        condition    = TagCondition.PDV_CLOSED,
    ),
)

/**
 * Evalúa si un tag debe mostrarse para un stop dado.
 *
 * @param tag       configuración del tag
 * @param stop      entidad del stop
 * @param kpiValues mapa kpiId → valueText del stop (para condiciones KPI)
 */
fun evaluateTag(
    tag:       StopTagConfig,
    stop:      StopEntity,
    kpiValues: Map<String, String> = emptyMap(),
): Boolean {
    if (!tag.enabled) return false
    return when (tag.condition) {
        TagCondition.ALWAYS           -> true
        TagCondition.STATUS_DONE      -> stop.status == "done"
        TagCondition.STATUS_PENDING   -> stop.status == "pending" || stop.status == "visiting"
        TagCondition.PDV_OPEN         -> stop.status == "done" && stop.pdvOpen
        TagCondition.PDV_CLOSED       -> stop.status == "done" && !stop.pdvOpen
        TagCondition.RESULT_IS        -> stop.visitResult == tag.conditionValue
        TagCondition.KPI_ABOVE        -> {
            val v = kpiValues[tag.conditionKpiId]?.toDoubleOrNull() ?: return false
            v > tag.conditionThreshold
        }
        TagCondition.KPI_BELOW        -> {
            val v = kpiValues[tag.conditionKpiId]?.toDoubleOrNull() ?: return false
            v < tag.conditionThreshold
        }
        TagCondition.KPI_BOOL_TRUE    -> kpiValues[tag.conditionKpiId]?.lowercase() == "true"
        TagCondition.DAYS_SINCE_VISIT -> {
            val visitedAt = stop.visitedAt ?: return false
            val visitDate = runCatching {
                java.time.LocalDate.parse(visitedAt.substring(0, 10))
            }.getOrNull() ?: return false
            val days = java.time.temporal.ChronoUnit.DAYS.between(visitDate, java.time.LocalDate.now())
            days >= tag.conditionThreshold
        }
    }
}

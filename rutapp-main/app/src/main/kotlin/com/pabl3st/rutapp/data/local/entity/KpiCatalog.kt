package com.pabl3st.rutapp.data.local.entity

/**
 * Catálogo de KPIs predefinidos por sector.
 * Se insertan en BD la primera vez que el usuario elige un sector
 * o cuando se detecta que el sector no tiene KPIs aún (countSystem == 0).
 */
object KpiCatalog {

    // ── KPIs comunes a todos los sectores ────────────────────
    val COMMON = listOf(
        kpi("common_resultado",    "common", "Resultado visita",   "select",  null,
            options = """["contactado","no_estaba","volvemos","rechazado"]""",
            required = true, section = "general", order = 0),
        kpi("common_duracion",     "common", "Duración (min)",     "number",  "min",  section = "general", order = 1),
        kpi("common_notas",        "common", "Notas",              "text",    null,   section = "notas",    order = 0),
        kpi("common_next_action",  "common", "Próxima acción",     "text",    null,   section = "notas",    order = 1),
    )

    // ── Telco ────────────────────────────────────────────────
    val TELCO = listOf(
        kpi("telco_activaciones",  "telco", "Activaciones",        "number", "ud",  required = true, section = "objetivos", order = 0),
        kpi("telco_bono_eur",      "telco", "Importe bono (€)",    "number", "€",   section = "objetivos", order = 1),
        kpi("telco_renovaciones",  "telco", "Renovaciones",        "number", "ud",  section = "objetivos", order = 2),
        kpi("telco_portabilidades","telco", "Portabilidades ent.", "number", "ud",  section = "objetivos", order = 3),
        kpi("telco_churns",        "telco", "Churns (bajas)",      "number", "ud",  section = "objetivos", order = 4),
        kpi("telco_stock_sims",    "telco", "Stock SIMs",          "number", "ud",  section = "pedidos",   order = 0),
        kpi("telco_pedido_eur",    "telco", "Pedido (€)",          "number", "€",   section = "pedidos",   order = 1),
        kpi("telco_plus",          "telco", "Plus conseguido",     "boolean", null, section = "objetivos", order = 5),
        kpi("telco_tv",            "telco", "Televisión (ud)",     "number", "ud",  visible = false, section = "objetivos", order = 6),
    )

    // ── Farmacia / Parafarmacia ───────────────────────────────
    val FARMA = listOf(
        kpi("farma_unidades",      "farma", "Unidades vendidas",   "number", "ud",  required = true, section = "objetivos", order = 0),
        kpi("farma_pedido_eur",    "farma", "Pedido (€)",          "number", "€",   required = true, section = "pedidos",   order = 0),
        kpi("farma_referencias",   "farma", "Refs. activas",       "number", "ud",  section = "objetivos", order = 1),
        kpi("farma_facing",        "farma", "Facing en lineal",    "number", "ud",  section = "objetivos", order = 2),
        kpi("farma_caducidades",   "farma", "Caducidades retir.",  "number", "ud",  section = "pedidos",   order = 1),
        kpi("farma_devoluciones",  "farma", "Devoluciones",        "number", "ud",  visible = false, section = "pedidos", order = 2),
        kpi("farma_promo_activa",  "farma", "Promoción activa",    "boolean", null, section = "objetivos", order = 3),
    )

    // ── Distribución / Logística ──────────────────────────────
    val DISTRIBUCION = listOf(
        kpi("dist_pedido_eur",     "distribucion", "Pedido (€)",          "number", "€",  required = true, section = "pedidos",   order = 0),
        kpi("dist_referencias",    "distribucion", "Refs. pedidas",       "number", "ud", section = "pedidos",   order = 1),
        kpi("dist_incidencias",    "distribucion", "Incidencias logíst.", "number", "ud", section = "general",   order = 0),
        kpi("dist_exposicion",     "distribucion", "Exposición OK",       "boolean", null, section = "objetivos", order = 0),
        kpi("dist_competencia",    "distribucion", "Acción competencia",  "text",   null,  visible = false, section = "notas", order = 2),
    )

    // ── Retail / Gran consumo ─────────────────────────────────
    val RETAIL = listOf(
        kpi("retail_sellout",      "retail", "Sell-out (ud)",       "number", "ud", required = true, section = "objetivos", order = 0),
        kpi("retail_pedido_eur",   "retail", "Pedido reposición €", "number", "€",  section = "pedidos",   order = 0),
        kpi("retail_rotacion",     "retail", "Rotación (días)",     "number", "d",  section = "objetivos", order = 1),
        kpi("retail_promo",        "retail", "Promociones activas", "number", "ud", section = "objetivos", order = 2),
        kpi("retail_oos",          "retail", "Rotura de stock",     "boolean", null, section = "objetivos", order = 3),
        kpi("retail_competencia",  "retail", "Precio competencia €","number", "€",  visible = false, section = "notas", order = 2),
    )

    fun forSector(sector: String): List<KpiDefinitionEntity> = when (sector) {
        "telco"        -> COMMON + TELCO
        "farma"        -> COMMON + FARMA
        "distribucion" -> COMMON + DISTRIBUCION
        "retail"       -> COMMON + RETAIL
        else           -> COMMON   // custom: solo los comunes, el usuario añade el resto
    }

    private fun kpi(
        id: String, sector: String, label: String, type: String, unit: String?,
        options: String? = null, required: Boolean = false, visible: Boolean = true,
        section: String = "general", order: Int = 0,
    ) = KpiDefinitionEntity(
        id          = id,
        accountId   = 0,
        sector      = sector,
        label       = label,
        type        = type,
        unit        = unit,
        options     = options,
        required    = required,
        visible     = visible,
        orderIndex  = order,
        section     = section,
        isSystem    = true,
    )
}

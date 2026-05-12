package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.dao.BusinessProfileDao
import com.pabl3st.rutapp.data.local.dao.KpiDefinitionDao
import com.pabl3st.rutapp.data.local.entity.BusinessProfileEntity
import com.pabl3st.rutapp.data.local.entity.KpiCatalog
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import com.pabl3st.rutapp.data.session.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessProfileRepository @Inject constructor(
    private val profileDao: BusinessProfileDao,
    private val kpiDao:     KpiDefinitionDao,
    private val session:    SessionManager,
) {
    val accountId: Int get() = session.accountId

    // ── Perfil ────────────────────────────────────────────────

    fun observeProfile(): Flow<BusinessProfileEntity?> =
        profileDao.observe(accountId)

    suspend fun getOrCreateProfile(): BusinessProfileEntity {
        return profileDao.get(accountId) ?: BusinessProfileEntity(accountId = accountId).also {
            profileDao.upsert(it)
        }
    }

    /** Cambia el sector y carga los KPIs predefinidos si no existen ya */
    suspend fun setSector(sector: String) {
        val profile = BusinessProfileEntity(
            accountId = accountId,
            sector    = sector,
            name      = sectorLabel(sector),
        )
        profileDao.upsert(profile)
        seedKpisIfNeeded(sector)
    }

    // ── KPIs ─────────────────────────────────────────────────

    fun observeActiveKpis(): Flow<List<KpiDefinitionEntity>> {
        val repoAccountId = accountId
        return profileDao.observe(repoAccountId).flatMapLatest { profile ->
            val sector = profile?.sector?.takeIf { it.isNotBlank() } ?: "telco"
            kpiDao.observeActive(repoAccountId, sector)
        }
    }

    fun observeActiveKpis(sector: String): Flow<List<KpiDefinitionEntity>> =
        kpiDao.observeActive(accountId, sector)

    fun observeAllKpis(sector: String): Flow<List<KpiDefinitionEntity>> =
        kpiDao.observeAll(accountId, sector)

    /** Devuelve lista síncrona de KPIs visibles del sector — usado en buildSectorKpis */
    suspend fun getVisibleKpisForSector(sector: String): List<KpiDefinitionEntity> {
        // Sembrar KPIs predefinidos si el sector no tiene ninguno aún (primer uso)
        seedKpisIfNeeded(sector)
        return kpiDao.getVisible(accountId, sector)
    }

    /** Semilla: inserta KPIs predefinidos del sector si no existen */
    suspend fun seedKpisIfNeeded(sector: String) {
        val count = kpiDao.countSystem(sector) +
                    kpiDao.countSystem("common")
        if (count == 0) {
            kpiDao.upsertAll(KpiCatalog.forSector(sector))
        }
    }

    /** Asegura que los KPIs comunes siempre existen */
    suspend fun ensureCommonKpis() {
        val count = kpiDao.countSystem("common")
        if (count == 0) kpiDao.upsertAll(KpiCatalog.COMMON)
    }

    // ── Acciones del editor de KPIs ───────────────────────────

    suspend fun setKpiVisible(id: String, visible: Boolean) =
        kpiDao.setVisible(id, visible)

    suspend fun reorderKpi(id: String, newOrder: Int) =
        kpiDao.setOrder(id, newOrder)

    suspend fun addCustomKpi(
        label: String,
        type: String,
        unit: String?,
        options: String?,
        required: Boolean,
        section: String,
    ) {
        val profile = getOrCreateProfile()
        val id = "custom_${UUID.randomUUID().toString().take(8)}"
        kpiDao.upsert(
            KpiDefinitionEntity(
                id         = id,
                accountId  = accountId,
                sector     = "custom",
                label      = label,
                type       = type,
                unit       = unit,
                options    = options,
                required   = required,
                visible    = true,
                orderIndex = 999,
                section    = section,
                isSystem   = false,
            )
        )
    }

    suspend fun deleteCustomKpi(id: String) = kpiDao.deleteCustom(id)

    // ── Helpers ───────────────────────────────────────────────

    fun sectorLabel(sector: String) = when (sector) {
        "telco"        -> "Telecomunicaciones"
        "farma"        -> "Farmacia / Parafarmacia"
        "distribucion" -> "Distribución"
        "retail"       -> "Retail / Gran consumo"
        else           -> "Personalizado"
    }

    val sectors = listOf("telco", "farma", "distribucion", "retail", "custom")
}

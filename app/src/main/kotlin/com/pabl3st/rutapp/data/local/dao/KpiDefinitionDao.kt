package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KpiDefinitionDao {

    /** KPIs activos (visibles) para un account — sistema + custom del account */
    @Query("""
        SELECT * FROM kpi_definitions
        WHERE (accountId = 0 OR accountId = :accountId)
          AND (sector = :sector OR sector = 'common')
          AND visible = 1
        ORDER BY orderIndex ASC
    """)
    fun observeActive(accountId: Int, sector: String): Flow<List<KpiDefinitionEntity>>

    /** Consulta suspend (no Flow) — para loadKpiFields en VisitaViewModel */
    @Query("""
        SELECT * FROM kpi_definitions
        WHERE (accountId = 0 OR accountId = :accountId)
          AND (sector = :sector OR sector = 'common')
          AND visible = 1
        ORDER BY orderIndex ASC
    """)
    suspend fun getVisible(accountId: Int, sector: String): List<KpiDefinitionEntity>

    /** Todos los KPIs del account incluidos ocultos — para el editor */
    @Query("""
        SELECT * FROM kpi_definitions
        WHERE (accountId = 0 OR accountId = :accountId)
          AND (sector = :sector OR sector = 'common')
        ORDER BY section ASC, orderIndex ASC
    """)
    fun observeAll(accountId: Int, sector: String): Flow<List<KpiDefinitionEntity>>

    @Query("SELECT * FROM kpi_definitions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): KpiDefinitionEntity?

    @Upsert
    suspend fun upsert(kpi: KpiDefinitionEntity)

    @Upsert
    suspend fun upsertAll(kpis: List<KpiDefinitionEntity>)

    @Query("DELETE FROM kpi_definitions WHERE id = :id AND isSystem = 0")
    suspend fun deleteCustom(id: String)

    @Query("UPDATE kpi_definitions SET visible = :visible WHERE id = :id")
    suspend fun setVisible(id: String, visible: Boolean)

    @Query("UPDATE kpi_definitions SET orderIndex = :order WHERE id = :id")
    suspend fun setOrder(id: String, order: Int)

    @Query("SELECT COUNT(*) FROM kpi_definitions WHERE accountId = 0 AND sector = :sector")
    suspend fun countSystem(sector: String): Int
}

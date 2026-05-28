package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity

@Dao
interface KpiValueDao {

    @Query("SELECT * FROM kpi_values WHERE stopUid = :stopUid")
    suspend fun getByStop(stopUid: String): List<KpiValueEntity>

    /**
     * Devuelve el último total acumulado de cada KPI del stop.
     * Los KPIs son acumulativos (cada visita guarda el total vigente del PDV),
     * así que "el valor actual" es el de la visita más reciente.
     *
     * Toma la fila por cada kpiId cuya stop_visits.visitDate sea la más alta.
     * Si dos visitas tienen la misma fecha, devuelve un resultado estable
     * (la de updatedAt mayor por desempate).
     */
    @Query("""
        SELECT kv.* FROM kpi_values kv
        JOIN stop_visits sv ON sv.uid = kv.visitUid
        WHERE kv.stopUid = :stopUid
          AND sv.deletedAt IS NULL
          AND sv.visitDate = (
              SELECT MAX(sv2.visitDate)
              FROM kpi_values kv2
              JOIN stop_visits sv2 ON sv2.uid = kv2.visitUid
              WHERE kv2.stopUid = :stopUid
                AND kv2.kpiId   = kv.kpiId
                AND sv2.deletedAt IS NULL
          )
    """)
    suspend fun getLastTotalsByStop(stopUid: String): List<KpiValueEntity>

    /** Carga los kpi_values de múltiples stops — para lógica acumulativa mensual */
    @Query("SELECT * FROM kpi_values WHERE stopUid IN (:stopUids)")
    suspend fun getByStops(stopUids: List<String>): List<KpiValueEntity>

    /** Flow reactivo de todos los kpi_values de una lista de stops — para tags en tiempo real */
    @Query("SELECT * FROM kpi_values WHERE stopUid IN (:stopUids)")
    fun observeByStops(stopUids: List<String>): kotlinx.coroutines.flow.Flow<List<KpiValueEntity>>

    @Query("SELECT * FROM kpi_values WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<KpiValueEntity>

    @Upsert
    suspend fun upsertAll(values: List<KpiValueEntity>)

    @Upsert
    suspend fun upsert(value: KpiValueEntity)

    @Query("UPDATE kpi_values SET syncStatus = 'synced' WHERE stopUid = :stopUid")
    suspend fun markSynced(stopUid: String)

    @Query("DELETE FROM kpi_values WHERE stopUid = :stopUid")
    suspend fun deleteByStop(stopUid: String)

    /** Borra TODOS los KPI values cuyos stops pertenezcan a esta cuenta.
     *  Se hace por subquery porque KpiValueEntity no tiene accountId directo. */
    @Query("""
        DELETE FROM kpi_values
        WHERE stopUid IN (SELECT uid FROM stops WHERE accountId = :accountId)
    """)
    suspend fun deleteAllByAccount(accountId: Int)
}



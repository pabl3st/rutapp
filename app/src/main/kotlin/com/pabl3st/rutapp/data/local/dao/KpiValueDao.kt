package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity

@Dao
interface KpiValueDao {

    @Query("SELECT * FROM kpi_values WHERE stopUid = :stopUid")
    suspend fun getByStop(stopUid: String): List<KpiValueEntity>

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
}



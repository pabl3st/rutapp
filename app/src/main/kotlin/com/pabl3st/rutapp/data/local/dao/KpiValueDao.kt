package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity

@Dao
interface KpiValueDao {

    @Query("SELECT * FROM kpi_values WHERE stopUid = :stopUid")
    suspend fun getByStop(stopUid: String): List<KpiValueEntity>

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

package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.StopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StopDao {

    @Query("SELECT * FROM stops WHERE routeUid = :routeUid AND deletedAt IS NULL ORDER BY orderIndex ASC")
    fun observeByRoute(routeUid: String): Flow<List<StopEntity>>

    @Query("SELECT * FROM stops WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): StopEntity?

    @Query("SELECT * FROM stops WHERE syncStatus = 'pending' OR syncStatus = 'error'")
    suspend fun getPendingSync(): List<StopEntity>

    @Upsert
    suspend fun upsert(stop: StopEntity)

    @Upsert
    suspend fun upsertAll(stops: List<StopEntity>)

    @Query("UPDATE stops SET syncStatus = :status, syncedAt = :at WHERE uid = :uid")
    suspend fun updateSyncStatus(uid: String, status: String, at: String?)

    @Query("UPDATE stops SET status = :status, visitedAt = :at, updatedAt = :updatedAt, syncStatus = 'pending' WHERE uid = :uid")
    suspend fun updateStatus(uid: String, status: String, at: String?, updatedAt: String)

    @Query("UPDATE stops SET status = 'visiting', syncStatus = 'pending' WHERE uid = :uid AND status = 'pending'")
    suspend fun markVisiting(uid: String)

    @Query("""UPDATE stops SET status = 'done', visitedAt = :at, visitResult = :result,
        notes = :notes, nextAction = :nextAction, updatedAt = :at, syncStatus = 'pending'
        WHERE uid = :uid""")
    suspend fun updateVisitResult(uid: String, result: String, notes: String?, nextAction: String?, at: String)
}

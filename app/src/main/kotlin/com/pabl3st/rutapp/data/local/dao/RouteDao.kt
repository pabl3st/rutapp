package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("SELECT * FROM routes WHERE deletedAt IS NULL AND userId = :userId ORDER BY dateAssigned DESC")
    fun observeByUser(userId: Int): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE deletedAt IS NULL AND accountId = :accountId ORDER BY dateAssigned DESC, userId ASC")
    fun observeByAccount(accountId: Int): Flow<List<RouteEntity>>

    @Query("""
        SELECT * FROM routes
        WHERE deletedAt IS NULL
          AND userId = :userId
          AND (dateAssigned = :date
               OR scheduledDates = :date
               OR scheduledDates LIKE :date || ',%'
               OR scheduledDates LIKE '%,' || :date || ',%'
               OR scheduledDates LIKE '%,' || :date)
        ORDER BY name ASC
    """)
    fun observeByDate(userId: Int, date: String): Flow<List<RouteEntity>>

    @Query("""
        SELECT * FROM routes
        WHERE deletedAt IS NULL
          AND accountId = :accountId
          AND (dateAssigned = :date
               OR scheduledDates = :date
               OR scheduledDates LIKE :date || ',%'
               OR scheduledDates LIKE '%,' || :date || ',%'
               OR scheduledDates LIKE '%,' || :date)
        ORDER BY userId ASC, name ASC
    """)
    fun observeByDateForAccount(accountId: Int, date: String): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): RouteEntity?

    @Query("SELECT * FROM routes WHERE syncStatus = 'pending' OR syncStatus = 'error'")
    suspend fun getPendingSync(): List<RouteEntity>

    @Upsert
    suspend fun upsert(route: RouteEntity)

    @Upsert
    suspend fun upsertAll(routes: List<RouteEntity>)

    @Query("UPDATE routes SET syncStatus = :status, syncedAt = :at WHERE uid = :uid")
    suspend fun updateSyncStatus(uid: String, status: String, at: String?)

    @Query("DELETE FROM routes WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}

package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

/** Proyección de conteo de paradas por ruta — resultado de observeStopCounts(). */
data class RouteStopCount(
    val routeUid: String,
    val total: Int,
    val done: Int,
)

@Dao
interface RouteDao {

    @Query("SELECT * FROM routes WHERE deletedAt IS NULL AND userId = :userId ORDER BY dateAssigned DESC")
    fun observeByUser(userId: Int): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE deletedAt IS NULL AND accountId = :accountId ORDER BY dateAssigned DESC, userId ASC")
    fun observeByAccount(accountId: Int): Flow<List<RouteEntity>>

    /** Rutas de un conjunto de usuarios (agentes bajo un manager) */
    @Query("SELECT * FROM routes WHERE deletedAt IS NULL AND userId IN (:userIds) ORDER BY dateAssigned DESC")
    fun observeByUserIds(userIds: List<Int>): Flow<List<RouteEntity>>

    /** Rutas del día de un conjunto de usuarios */
    @Query("""
        SELECT * FROM routes
        WHERE deletedAt IS NULL
          AND userId IN (:userIds)
          AND (dateAssigned = :date
               OR scheduledDates = :date
               OR scheduledDates LIKE :date || ',%'
               OR scheduledDates LIKE '%,' || :date || ',%'
               OR scheduledDates LIKE '%,' || :date)
        ORDER BY name ASC
    """)
    fun observeByDateForUserIds(userIds: List<Int>, date: String): Flow<List<RouteEntity>>

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

    /**
     * Conteo de paradas por ruta — total y completadas — calculado en vivo
     * sobre la tabla stops local. Refleja el estado real al instante: marcar
     * una parada como visitada actualiza el contador sin esperar al servidor.
     */
    @Query("""
        SELECT routeUid AS routeUid,
               COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN status = 'done' THEN 1 ELSE 0 END), 0) AS done
        FROM stops
        WHERE deletedAt IS NULL
        GROUP BY routeUid
    """)
    fun observeStopCounts(): Flow<List<RouteStopCount>>

    @Query("SELECT * FROM routes WHERE name = :name AND userId = :userId AND deletedAt IS NULL LIMIT 1")
    suspend fun getByNameAndUser(name: String, userId: Int): RouteEntity?

    @Query("SELECT * FROM routes WHERE syncStatus = 'pending' OR syncStatus = 'error'")
    suspend fun getPendingSync(): List<RouteEntity>

    @Upsert
    suspend fun upsert(route: RouteEntity)

    @Upsert
    suspend fun upsertAll(routes: List<RouteEntity>)

    @Query("UPDATE routes SET syncStatus = :status, syncedAt = :at WHERE uid = :uid")
    suspend fun updateSyncStatus(uid: String, status: String, at: String?)

    @Query("UPDATE routes SET status = :status, updatedAt = :now, syncStatus = 'pending' WHERE uid = :uid")
    suspend fun updateStatus(uid: String, status: String, now: String)

    @Query("DELETE FROM routes WHERE accountId = :accountId")
    suspend fun deleteAllByAccount(accountId: Int)

    @Query("DELETE FROM routes WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}

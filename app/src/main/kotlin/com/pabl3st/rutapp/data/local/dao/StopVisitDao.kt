package com.pabl3st.rutapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pabl3st.rutapp.data.local.entity.StopVisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StopVisitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(visit: StopVisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(visits: List<StopVisitEntity>)

    /** Visitas de una ruta en una fecha concreta (para mostrar el estado de cada PDV ese día) */
    @Query("""
        SELECT * FROM stop_visits
        WHERE routeUid = :routeUid AND visitDate = :date AND deletedAt IS NULL
    """)
    fun observeByRouteAndDate(routeUid: String, date: String): Flow<List<StopVisitEntity>>

    /** Una visita específica por (stopUid, visitDate) */
    @Query("""
        SELECT * FROM stop_visits
        WHERE stopUid = :stopUid AND visitDate = :date AND deletedAt IS NULL
        LIMIT 1
    """)
    suspend fun getByStopAndDate(stopUid: String, date: String): StopVisitEntity?

    /** Una visita específica por uid */
    @Query("SELECT * FROM stop_visits WHERE uid = :uid AND deletedAt IS NULL LIMIT 1")
    suspend fun getByUid(uid: String): StopVisitEntity?

    /** Todas las visitas de un stop (histórico completo, descendente por fecha) */
    @Query("""
        SELECT * FROM stop_visits
        WHERE stopUid = :stopUid AND deletedAt IS NULL
        ORDER BY visitDate DESC
    """)
    fun observeByStop(stopUid: String): Flow<List<StopVisitEntity>>

    /** Todas las visitas pendientes de sync (para el SyncWorker) */
    @Query("SELECT * FROM stop_visits WHERE syncStatus = 'pending' AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<StopVisitEntity>

    @Query("UPDATE stop_visits SET syncStatus = :status, syncedAt = :syncedAt WHERE uid = :uid")
    suspend fun markSynced(uid: String, status: String, syncedAt: String)

    /** Fechas únicas con visitas para una ruta (fallback si route.scheduledDates está vacío) */
    @Query("""
        SELECT DISTINCT visitDate FROM stop_visits
        WHERE routeUid = :routeUid AND deletedAt IS NULL
        ORDER BY visitDate ASC
    """)
    suspend fun getDistinctDatesByRoute(routeUid: String): List<String>

    /** Borra TODAS las visitas de una cuenta. Usado por clearAllRoutes para evitar
     *  visitas huérfanas que apunten a stops ya eliminados. */
    @Query("DELETE FROM stop_visits WHERE accountId = :accountId")
    suspend fun deleteAllByAccount(accountId: Int)
}

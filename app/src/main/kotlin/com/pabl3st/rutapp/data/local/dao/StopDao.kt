package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.StopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StopDao {

    // ── Observar stops de una ruta ────────────────────────────
    @Query("SELECT * FROM stops WHERE routeUid = :routeUid AND deletedAt IS NULL ORDER BY orderIndex ASC")
    fun observeByRoute(routeUid: String): Flow<List<StopEntity>>

    // ── Mapa global: stops de varias rutas ───────────────────
    @Query("SELECT * FROM stops WHERE routeUid IN (:routeUids) AND deletedAt IS NULL ORDER BY orderIndex ASC")
    fun observeByRouteUids(routeUids: List<String>): Flow<List<StopEntity>>

    /** Solo stops con GPS válido — para markers en mapa global */
    @Query("""
        SELECT * FROM stops
        WHERE routeUid IN (:routeUids)
          AND deletedAt IS NULL
          AND lat IS NOT NULL AND lng IS NOT NULL
          AND lat != 0.0   AND lng != 0.0
        ORDER BY status ASC, orderIndex ASC
    """)
    fun observeWithGpsByRouteUids(routeUids: List<String>): Flow<List<StopEntity>>

    // ── Biblioteca ────────────────────────────────────────────
    @Query("SELECT * FROM stops WHERE accountId = :accountId AND deletedAt IS NULL ORDER BY name ASC")
    fun observeAll(accountId: Int): Flow<List<StopEntity>>

    @Query("SELECT * FROM stops WHERE accountId = :accountId AND deletedAt IS NULL AND (lat IS NULL OR lng IS NULL OR lat = 0.0 OR lng = 0.0) ORDER BY name ASC")
    fun observeWithoutGps(accountId: Int): Flow<List<StopEntity>>

    @Query("SELECT * FROM stops WHERE accountId = :accountId AND deletedAt IS NULL AND routeUid NOT IN (SELECT uid FROM routes WHERE deletedAt IS NULL) ORDER BY name ASC")
    fun observeOrphaned(accountId: Int): Flow<List<StopEntity>>

    // ── Lecturas puntuales ────────────────────────────────────
    @Query("SELECT * FROM stops WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): StopEntity?

    @Query("SELECT * FROM stops WHERE syncStatus = 'pending' OR syncStatus = 'error'")
    suspend fun getPendingSync(): List<StopEntity>

    // ── Escritura ─────────────────────────────────────────────
    @Upsert
    suspend fun upsert(stop: StopEntity)

    @Upsert
    suspend fun upsertAll(stops: List<StopEntity>)

    // ── Actualizaciones de estado ─────────────────────────────
    @Query("UPDATE stops SET syncStatus = :status, syncedAt = :at WHERE uid = :uid")
    suspend fun updateSyncStatus(uid: String, status: String, at: String?)

    @Query("UPDATE stops SET status = :status, visitedAt = :at, updatedAt = :updatedAt, syncStatus = 'pending' WHERE uid = :uid")
    suspend fun updateStatus(uid: String, status: String, at: String?, updatedAt: String)

    @Query("""
        UPDATE stops SET status = 'pending', visitResult = NULL, visitedAt = NULL,
        notes = NULL, nextAction = NULL, updatedAt = :at, syncStatus = 'pending'
        WHERE uid = :uid
    """)
    suspend fun resetForNewVisit(uid: String, at: String)

    @Query("UPDATE stops SET status = 'visiting', syncStatus = 'pending' WHERE uid = :uid AND status = 'pending'")
    suspend fun markVisiting(uid: String)

    @Query("UPDATE stops SET lat = :lat, lng = :lng, updatedAt = :at, syncStatus = 'pending' WHERE uid = :uid")
    suspend fun updateCoords(uid: String, lat: Double, lng: Double, at: String)

    @Query("""
        UPDATE stops SET status = 'done', visitedAt = :at, visitResult = :result,
        notes = :notes, nextAction = :nextAction, pdvOpen = :pdvOpen, updatedAt = :at, syncStatus = 'pending'
        WHERE uid = :uid
    """)
    suspend fun updateVisitResult(uid: String, result: String, notes: String?, nextAction: String?, pdvOpen: Boolean, at: String)

    // ── Reordenación bulk ─────────────────────────────────────
    @Query("UPDATE stops SET orderIndex = :orderIndex, updatedAt = :at, syncStatus = 'pending' WHERE uid = :uid")
    suspend fun updateOrderIndex(uid: String, orderIndex: Int, at: String)
}


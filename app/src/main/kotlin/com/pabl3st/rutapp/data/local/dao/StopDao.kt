package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.StopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StopDao {

    // ── Observar stops de una ruta ────────────────────────────
    @Query("SELECT * FROM stops WHERE routeUid = :routeUid AND deletedAt IS NULL ORDER BY orderIndex ASC")
    fun observeByRoute(routeUid: String): Flow<List<StopEntity>>

    /** Stops de la ruta de una fecha concreta (para el modelo de informe independiente por día) */
    @Query("""
        SELECT * FROM stops
        WHERE routeUid = :routeUid
          AND deletedAt IS NULL
          AND (dateAssigned = :date OR dateAssigned IS NULL)
        ORDER BY orderIndex ASC
    """)
    fun observeByRouteAndDate(routeUid: String, date: String): Flow<List<StopEntity>>

    /** Todas las fechas únicas de stops de una ruta (para el selector de fecha en RouteDetail) */
    @Query("""
        SELECT DISTINCT dateAssigned FROM stops
        WHERE routeUid = :routeUid
          AND deletedAt IS NULL
          AND dateAssigned IS NOT NULL
        ORDER BY dateAssigned ASC
    """)
    suspend fun getDistinctDates(routeUid: String): List<String>

    @Query("SELECT * FROM stops WHERE routeUid = :routeUid AND deletedAt IS NULL ORDER BY orderIndex ASC")
    suspend fun getByRoute(routeUid: String): List<StopEntity>

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

    @Query("DELETE FROM stops WHERE accountId = :accountId")
    suspend fun deleteAllByAccount(accountId: Int)

    @Query("""
        SELECT * FROM stops
        WHERE routeUid = :routeUid
          AND externalId = :externalId
          AND dateAssigned = :dateAssigned
          AND deletedAt IS NULL
        LIMIT 1
    """)
    suspend fun getByExternalIdDateAndRoute(
        routeUid:     String,
        externalId:   String,
        dateAssigned: String,
    ): StopEntity?

    @Query("""
        SELECT * FROM stops
        WHERE routeUid = :routeUid
          AND externalId = :externalId
          AND deletedAt IS NULL
        LIMIT 1
    """)
    suspend fun getByExternalIdAndRoute(
        routeUid:   String,
        externalId: String,
    ): StopEntity?

    /**
     * Stops del mismo PDV (mismo externalId) visitados en el mes dado.
     * Usado para KPIs acumulativos: cargar el valor más alto del mes al abrir formulario.
     * monthPrefix = "YYYY-MM" — ej: "2026-05"
     */
    /** Historial completo de visitas al PDV (mismo externalId), más reciente primero */
    @Query("""
        SELECT * FROM stops
        WHERE accountId  = :accountId
          AND externalId = :externalId
          AND externalId IS NOT NULL
          AND status     = 'done'
          AND deletedAt  IS NULL
        ORDER BY visitedAt DESC
        LIMIT 20
    """)
    suspend fun getVisitHistoryByExternalId(
        accountId:  Int,
        externalId: String,
    ): List<StopEntity>

    /** Historial para stops sin externalId — por uid directo */
    @Query("""
        SELECT * FROM stops
        WHERE uid = :stopUid AND status = 'done'
        LIMIT 1
    """)
    suspend fun getVisitHistoryByUid(stopUid: String): List<StopEntity>

    @Query("""
        SELECT * FROM stops
        WHERE accountId  = :accountId
          AND externalId = :externalId
          AND externalId IS NOT NULL
          AND status     = 'done'
          AND visitedAt  LIKE :monthPrefix || '%'
          AND uid        != :excludeUid
          AND deletedAt  IS NULL
        ORDER BY visitedAt DESC
    """)
    suspend fun getDoneByExternalIdInMonth(
        accountId:   Int,
        externalId:  String,
        monthPrefix: String,
        excludeUid:  String,
    ): List<StopEntity>

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
        notes = NULL, nextAction = NULL, pdvInactive = 0, updatedAt = :at, syncStatus = 'pending'
        WHERE uid = :uid
    """)
    suspend fun resetForNewVisit(uid: String, at: String)

    @Query("UPDATE stops SET status = 'visiting', syncStatus = 'pending' WHERE uid = :uid AND status = 'pending'")
    suspend fun markVisiting(uid: String)

    @Query("""
        UPDATE stops SET
            checkInTs   = COALESCE(:checkInTs,  checkInTs),
            checkOutTs  = COALESCE(:checkOutTs, checkOutTs),
            gpsLatVisit = COALESCE(:gpsLat,     gpsLatVisit),
            gpsLngVisit = COALESCE(:gpsLng,     gpsLngVisit),
            syncStatus  = 'pending'
        WHERE uid = :uid
    """)
    suspend fun updateCheckInOut(uid: String, checkInTs: Long?, checkOutTs: Long?,
                                  gpsLat: Double?, gpsLng: Double?)

    @Query("UPDATE stops SET lat = :lat, lng = :lng, updatedAt = :at, syncStatus = 'pending' WHERE uid = :uid")
    suspend fun updateCoords(uid: String, lat: Double, lng: Double, at: String)

    @Query("""
        UPDATE stops SET status = 'done', visitedAt = :at, visitResult = :result,
        notes = :notes, nextAction = :nextAction, pdvOpen = :pdvOpen,
        pdvInactive = :pdvInactive, accountStatus = :accountStatus,
        updatedAt = :at, syncStatus = 'pending'
        WHERE uid = :uid
    """)
    suspend fun updateVisitResult(
        uid: String, result: String, notes: String?, nextAction: String?,
        pdvOpen: Boolean, pdvInactive: Boolean, accountStatus: String, at: String
    )

    // ── Quitar / vincular paradas ─────────────────────────────
    @Query("""
        UPDATE stops SET
            deletedAt  = :now,
            updatedAt  = :now,
            syncStatus = 'pending'
        WHERE uid = :stopUid
    """)
    suspend fun softDelete(stopUid: String, now: String)

    @Query("""
        UPDATE stops SET
            deletedAt  = :now,
            updatedAt  = :now,
            syncStatus = 'pending'
        WHERE routeUid = :routeUid AND deletedAt IS NULL
    """)
    suspend fun softDeleteByRoute(routeUid: String, now: String)

    /**
     * Desvincula la parada de su ruta SIN borrarla: conserva KPIs, informes,
     * resultado de visita y todo el historial. La parada queda huerfana (sin
     * ruta) y sin fecha asignada hasta que se incluya en otra ruta, momento
     * en que linkToRoute le pone la fecha de esa nueva ruta.
     *
     * routeUid = '' (no NULL) porque la columna es String no-nulo en
     * StopEntity; cambiarla exigiria migracion de esquema. La cadena vacia
     * ya encaja con observeOrphaned, que define huerfana como "routeUid que
     * no apunta a ninguna ruta viva", asi que la parada aparece sola en la
     * pestana 'Sin ruta' de la Biblioteca.
     *
     * Antes esto era un softDelete: quitar una parada de una ruta borraba el
     * PDV entero junto con sus datos rellenados.
     */
    @Query("""
        UPDATE stops SET
            routeUid     = '',
            dateAssigned = NULL,
            orderIndex   = 0,
            deletedAt    = NULL,
            updatedAt    = :now,
            syncStatus   = 'pending'
        WHERE uid = :stopUid
    """)
    suspend fun detachFromRoute(stopUid: String, now: String)

    @Query("""
        UPDATE stops SET
            routeUid     = :routeUid,
            dateAssigned = :dateAssigned,
            orderIndex   = :orderIndex,
            deletedAt    = NULL,
            updatedAt    = :now,
            syncStatus   = 'pending'
        WHERE uid = :stopUid
    """)
    suspend fun linkToRoute(
        stopUid:      String,
        routeUid:     String,
        dateAssigned: String,
        orderIndex:   Int,
        now:          String,
    )

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM stops WHERE routeUid = :routeUid AND deletedAt IS NULL")
    suspend fun nextOrderIndex(routeUid: String): Int

    // ── Reordenación bulk ─────────────────────────────────────
    @Query("UPDATE stops SET orderIndex = :orderIndex, updatedAt = :at, syncStatus = 'pending' WHERE uid = :uid")
    suspend fun updateOrderIndex(uid: String, orderIndex: Int, at: String)
}




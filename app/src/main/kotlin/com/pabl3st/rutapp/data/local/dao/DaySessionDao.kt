package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.DaySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DaySessionDao {

    @Query("SELECT * FROM day_sessions WHERE routeUid = :routeUid AND dateStr = :dateStr LIMIT 1")
    fun observe(routeUid: String, dateStr: String): Flow<DaySessionEntity?>

    @Query("SELECT * FROM day_sessions WHERE routeUid = :routeUid AND dateStr = :dateStr LIMIT 1")
    suspend fun get(routeUid: String, dateStr: String): DaySessionEntity?

    @Upsert
    suspend fun upsert(session: DaySessionEntity)

    @Query("UPDATE day_sessions SET state = :state, startedAt = :startedAt, pausedAt = :pausedAt, elapsedMs = :elapsedMs, updatedAt = :now WHERE routeUid = :routeUid AND dateStr = :dateStr")
    suspend fun updateState(routeUid: String, dateStr: String, state: String, startedAt: Long?, pausedAt: Long?, elapsedMs: Long, now: Long)

    @Query("UPDATE day_sessions SET distanceKm = :km, lastLat = :lat, lastLng = :lng, updatedAt = :now WHERE routeUid = :routeUid AND dateStr = :dateStr")
    suspend fun updateDistance(routeUid: String, dateStr: String, km: Double, lat: Double, lng: Double, now: Long)
}

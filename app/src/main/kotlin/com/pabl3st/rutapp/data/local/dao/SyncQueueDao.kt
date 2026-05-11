package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT 50")
    suspend fun getNext50(): List<SyncQueueEntity>

    // REPLACE: si ya existe (entity+entityUid+operation), sobreescribe con el payload más reciente
    // Esto implementa "last-write-wins" — solo el último estado se sube al servidor
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("UPDATE sync_queue SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun markFailed(id: Int, error: String)

    /** Elimina items que han fallado demasiadas veces — evita acumulación infinita */
    @Query("DELETE FROM sync_queue WHERE attempts >= :maxAttempts")
    suspend fun purgeExhausted(maxAttempts: Int = 5)

    /** Elimina items más antiguos que N días (creados antes de :cutoff ISO timestamp) */
    @Query("DELETE FROM sync_queue WHERE createdAt < :cutoff")
    suspend fun purgeOlderThan(cutoff: String)

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun count(): Int
}

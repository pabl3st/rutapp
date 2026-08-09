package com.pabl3st.rutapp.data.local.dao

import androidx.room.*
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT 50")
    suspend fun getNext50(): List<SyncQueueEntity>

    /** Selecciona próximo batch para subir, excluyendo items que han fallado
     *  repetidamente. Sin el filtro de attempts, un item que el server rechaza
     *  consistentemente se devolvería en cada iteración del loop de upload y
     *  bloquearía las ops sanas que vienen detrás. */
    //
    // maxAttempts DEBE coincidir con el umbral de purgeExhausted (20). Con 5
    // aqui y 20 alli habia una zona muerta: un op que fallaba 5 veces dejaba
    // de reintentarse pero seguia contando en count(), asi que la UI mostraba
    // "cola: 166" congelada y uploadPending devolvia exito con la cola llena
    // de ops invisibles. Si el fallo era del servidor (ej: date_assigned
    // vacio), al corregirlo esos ops NUNCA se recuperaban solos.
    @Query("SELECT * FROM sync_queue WHERE attempts < :maxAttempts ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getNextBatch(limit: Int = 50, maxAttempts: Int = 20): List<SyncQueueEntity>

    /** Reactiva ops agotados tras corregir la causa del fallo (ej: fix en el
     *  servidor). Sin esto quedarian bloqueados hasta que purgeExhausted los
     *  borrase, perdiendo el dato. */
    @Query("UPDATE sync_queue SET attempts = 0, lastError = NULL WHERE attempts >= :minAttempts")
    suspend fun resetExhausted(minAttempts: Int = 5): Int

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

    /** Elimina items más antiguos que :cutoffMillis (epoch millis).
     *
     *  BUG (ago 2026): este parámetro era un String ISO. createdAt es INTEGER
     *  (System.currentTimeMillis()) y SQLite ordena por clase de tipo antes
     *  que por valor: cualquier INTEGER < cualquier TEXT. La condición se
     *  cumplía SIEMPRE y la purga vaciaba la cola entera en cada sync, justo
     *  despues de que reEnqueueOrphans() la hubiera rellenado. Resultado:
     *  nada subia nunca al servidor y el sync reportaba exito.
     *  El tipo debe coincidir con el de la columna. */
    @Query("DELETE FROM sync_queue WHERE createdAt < :cutoffMillis")
    suspend fun purgeOlderThan(cutoffMillis: Long)

    @Query("SELECT entityUid FROM sync_queue")
    suspend fun getAllUids(): List<String>

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun count(): Int

    /** Vacía la cola entera. Usado por clearAllRoutes para descartar pushes
     *  pendientes de entidades que acaban de borrarse. */
    @Query("DELETE FROM sync_queue")
    suspend fun purgeAll()
}

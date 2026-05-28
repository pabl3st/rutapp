package com.pabl3st.rutapp.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicInteger

/**
 * Gateway central para `triggerSync()` en los Repositorios.
 *
 * Problema histórico (mayo 2026): cada Repositorio (Route/Stop/StopVisit/Jornada)
 * llamaba a su propio `WorkManager.enqueueUniqueWork(REPLACE)` por cada
 * operación encolada. En operaciones masivas como una importación XLS de
 * ~4500 entidades, eso se traducía en miles de cancelaciones encadenadas y
 * el worker NUNCA llegaba a ejecutar `runSync()`. El servidor se quedaba
 * sin recibir un solo `batch_sync`.
 *
 * Solución actual (opción A — focal, simple):
 * - Quien hace una operación masiva llama `beginBatch()` antes y `endBatch()`
 *   al terminar. Mientras el batch esté abierto, los `triggerSync()` que
 *   hagan los repositorios se SUPRIMEN (no se encolan en WorkManager).
 * - El que abrió el batch es responsable de disparar UN único `runSync()`
 *   explícito al final (típicamente vía `SyncRepository.runSync()`).
 *
 * Mejora futura sugerida (opción B — global, automática):
 * - Sustituir el flag por un debounce: `triggerSync()` solo encola si han
 *   pasado N ms desde la última llamada. Eso beneficiaría a cualquier
 *   escritura masiva sin necesidad de marcar batch_mode explícitamente.
 *   Implementación: guardar `lastTriggerMs` y comparar contra `now()`.
 *
 * El conteo es atómico y soporta batches anidados — útil si en el futuro
 * un importer dispara otro batch internamente (sin contar dobles fines).
 */
@Singleton
class SyncGateway @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val batchDepth = AtomicInteger(0)

    /** True si hay al menos un batch abierto. Mientras esto sea cierto, los
     *  triggerSync() de los repositorios se ignoran. */
    val isBatchActive: Boolean get() = batchDepth.get() > 0

    /** Abre un batch — el caller PROMETE llamar a [endBatch] al terminar y
     *  disparar `SyncRepository.runSync()` por su cuenta al final. */
    fun beginBatch() { batchDepth.incrementAndGet() }

    /** Cierra un batch. Si era el último abierto, queda activa la sincronización
     *  normal (cualquier `triggerSync()` posterior sí encolará el worker). */
    fun endBatch() {
        batchDepth.updateAndGet { d -> (d - 1).coerceAtLeast(0) }
    }

    /** Dispara el SyncWorker, salvo que haya un batch abierto. Reemplaza al
     *  triggerSync() local de cada repositorio: ahora todos pasan por aquí.
     *  La política sigue siendo REPLACE (último gana) para mantener el
     *  comportamiento fuera de batches. */
    fun trigger() {
        if (isBatchActive) return  // batch abierto: el caller hará runSync() al final
        runCatching {
            WorkManager.getInstance(appContext)
                .enqueueUniqueWork(
                    SyncWorker.WORK_NAME_ONDEMAND,
                    ExistingWorkPolicy.REPLACE,
                    SyncWorker.onDemandRequest(),
                )
        }
    }
}

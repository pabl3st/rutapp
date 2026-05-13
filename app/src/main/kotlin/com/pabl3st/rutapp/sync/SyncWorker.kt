package com.pabl3st.rutapp.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.pabl3st.rutapp.data.repository.SyncRepository
import com.pabl3st.rutapp.data.repository.SyncResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return when (syncRepository.runSync()) {
            SyncResult.Success       -> Result.success()
            SyncResult.NoAuth        -> Result.success()      // sin token — no reintentar
            SyncResult.Unauthorized  -> Result.success()      // 401 — OkHttp Authenticator se encarga
            SyncResult.UploadError   -> if (runAttemptCount < 3) Result.retry() else Result.failure()
            SyncResult.DownloadError -> if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "rutasapp_sync_periodic"
        const val WORK_NAME_ONDEMAND = "rutasapp_sync_ondemand"

        fun periodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

        fun onDemandRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
    }
}

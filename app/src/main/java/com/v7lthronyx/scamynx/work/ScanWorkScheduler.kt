package com.v7lthronyx.scamynx.work

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.v7lthronyx.scamynx.domain.model.ScanRequest
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) {

    fun enqueueScan(request: ScanRequest): UUID? {
        val inputData = try {
            ScanWorker.buildInputData(request)
        } catch (error: IllegalStateException) {
            // WorkManager Data payload exceeded 10 KB (common with base64 file inputs); skip background work.
            android.util.Log.w(TAG, "Skipping WorkManager enqueue for ${request.targetType}: ${error.message}")
            return null
        }

        val workRequest = OneTimeWorkRequestBuilder<ScanWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        val uniqueName = "scan-${request.targetType.name}-${request.rawInput.hashCode()}"
        workManager.enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )

        return workRequest.id
    }

    fun scheduleThreatFeedSync() {
        val workRequest = PeriodicWorkRequestBuilder<ThreatFeedSyncWorker>(
            12, TimeUnit.HOURS,
            2, TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            ThreatFeedSyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest,
        )
    }

    fun cancelScan(workId: UUID) {
        workManager.cancelWorkById(workId)
    }

    private companion object {
        private const val TAG = "ScanWorkScheduler"
    }
}

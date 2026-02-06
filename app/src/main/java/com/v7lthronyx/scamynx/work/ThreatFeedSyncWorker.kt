package com.v7lthronyx.scamynx.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.v7lthronyx.scamynx.domain.repository.ThreatFeedRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ThreatFeedSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val threatFeedRepository: ThreatFeedRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            threatFeedRepository.syncThreatFeeds()
            Log.d(TAG, "Threat feed sync completed successfully")
            Result.success()
        } catch (error: Exception) {
            Log.w(TAG, "Threat feed sync failed", error)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val UNIQUE_NAME = "ThreatFeedRefresh"
        private const val TAG = "ThreatFeedSyncWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}

package com.v7lthronyx.scamynx.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.v7lthronyx.scamynx.data.telemetry.TelemetryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TelemetrySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val telemetryRepository: TelemetryRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!telemetryRepository.isEnabled) {
            return Result.success()
        }

        return try {
            val flushResult = telemetryRepository.flush()
            if (flushResult.isSuccess) {
                Log.d(TAG, "Telemetry flush succeeded: ${flushResult.getOrNull()} events sent")
                Result.success()
            } else {
                Log.w(TAG, "Telemetry flush failed: ${flushResult.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (error: Exception) {
            Log.e(TAG, "Telemetry sync error (attempt $runAttemptCount)", error)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val UNIQUE_NAME = "TelemetrySync"
        private const val TAG = "TelemetrySyncWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}

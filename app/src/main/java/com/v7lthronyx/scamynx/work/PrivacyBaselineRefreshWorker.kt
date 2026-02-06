package com.v7lthronyx.scamynx.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.v7lthronyx.scamynx.data.privacyradar.storage.PrivacyBaselineRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PrivacyBaselineRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val baselineRefresher: PrivacyBaselineRefresher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = runCatching {
        baselineRefresher.refreshAll()
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )

    companion object {
        const val UNIQUE_NAME = "privacy-baseline-refresh"
    }
}

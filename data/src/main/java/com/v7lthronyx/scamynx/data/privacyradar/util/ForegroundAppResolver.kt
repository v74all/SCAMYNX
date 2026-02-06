package com.v7lthronyx.scamynx.data.privacyradar.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundAppResolver @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val usageStatsManager: UsageStatsManager? = ContextCompat.getSystemService(
        context,
        UsageStatsManager::class.java,
    )

    fun resolveForegroundPackage(): String? {
        val manager = usageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val events = manager.queryEvents(now - LOOKBACK_MS, now)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTimestamp = 0L
        while (events.getNextEvent(event)) {
            @Suppress("DEPRECATION")
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                if (event.timeStamp > latestTimestamp) {
                    latestTimestamp = event.timeStamp
                    latestPackage = event.packageName
                }
            }
        }
        return latestPackage
    }

    companion object {
        private const val LOOKBACK_MS = 7_000L
    }
}

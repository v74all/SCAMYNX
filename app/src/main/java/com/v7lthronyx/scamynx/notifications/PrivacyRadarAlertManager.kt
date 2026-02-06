package com.v7lthronyx.scamynx.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.v7lthronyx.scamynx.BuildConfig
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.privacyradar.coordinator.PrivacyRadarCoordinator
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyResourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class PrivacyRadarAlertManager @Inject constructor(
    private val coordinator: PrivacyRadarCoordinator,
    @ApplicationContext private val context: Context,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var started = false
    private val notificationManager by lazy { NotificationManagerCompat.from(context) }
    private val powerManager by lazy { context.getSystemService(PowerManager::class.java) }

    fun start() {
        if (!BuildConfig.PRIVACY_RADAR_ENABLED) return
        if (started) return
        if (powerManager?.isPowerSaveMode == true) {
            return
        }
        started = true
        ensureChannel()
        coordinator.start()
        scope.launch {
            coordinator.hotLaneEvents.collectLatest { event ->
                showNotification(event)
            }
        }
    }

    suspend fun stop() {
        scope.coroutineContext.cancelChildren()
        started = false
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.privacy_radar_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.privacy_radar_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification(event: PrivacyEvent) {
        val resourceLabel = resourceLabel(event.resourceType)
        val appLabel = resolveAppLabel(event.packageName)
        val title = context.getString(R.string.privacy_radar_alert_title, resourceLabel)
        val body = context.getString(R.string.privacy_radar_alert_body, appLabel, resourceLabel)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val notificationId = (event.packageName.hashCode() + event.timestamp.hashCode()).absoluteValue
        notificationManager.notify(notificationId, notification)
    }

    private fun resolveAppLabel(packageName: String): String = runCatching {
        val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
        val label = context.packageManager.getApplicationLabel(applicationInfo)
        label?.toString()
    }.getOrNull() ?: packageName

    private fun resourceLabel(resourceType: PrivacyResourceType): String = when (resourceType) {
        PrivacyResourceType.CAMERA -> context.getString(R.string.privacy_radar_resource_camera)
        PrivacyResourceType.MICROPHONE -> context.getString(R.string.privacy_radar_resource_microphone)
        PrivacyResourceType.LOCATION -> context.getString(R.string.privacy_radar_resource_location)
        PrivacyResourceType.PHISHING_URL -> context.getString(R.string.privacy_radar_resource_link)
        PrivacyResourceType.WIFI_NETWORK -> context.getString(R.string.privacy_radar_resource_wifi)
        else -> context.getString(R.string.privacy_radar_resource_generic)
    }

    companion object {
        private const val CHANNEL_ID = "privacy_radar_alerts"
    }
}

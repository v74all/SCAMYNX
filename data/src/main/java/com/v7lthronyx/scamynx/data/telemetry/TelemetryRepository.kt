package com.v7lthronyx.scamynx.data.telemetry

import android.os.Build
import com.v7lthronyx.scamynx.data.BuildConfig
import com.v7lthronyx.scamynx.data.db.TelemetryEventDao
import com.v7lthronyx.scamynx.data.db.TelemetryEventEntity
import com.v7lthronyx.scamynx.data.network.api.TelemetryApi
import com.v7lthronyx.scamynx.data.network.model.DeviceInfoDto
import com.v7lthronyx.scamynx.data.network.model.TelemetryBatchRequestDto
import com.v7lthronyx.scamynx.data.network.model.TelemetryEventDto
import com.v7lthronyx.scamynx.data.util.ApiCredentials
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface TelemetryRepository {

    /** Whether telemetry is currently enabled and configured */
    val isEnabled: Boolean

    /** Queue an event for sending. Will be batched automatically. */
    suspend fun trackEvent(
        eventType: TelemetryEventType,
        payload: Map<String, String> = emptyMap(),
        sessionId: String? = null,
    )

    /** Send all queued events immediately */
    suspend fun flush(): Result<Int>

    /** Track a scan completion event */
    suspend fun trackScanCompleted(
        sessionId: String,
        targetType: String,
        riskLevel: String,
        duration: Long,
    )

    /** Track a feature usage event */
    suspend fun trackFeatureUsed(featureName: String)

    /** Track an error event (anonymized) */
    suspend fun trackError(errorType: String, context: String? = null)
}

enum class TelemetryEventType(val value: String) {
    APP_LAUNCHED("app_launched"),
    SCAN_STARTED("scan_started"),
    SCAN_COMPLETED("scan_completed"),
    SCAN_FAILED("scan_failed"),
    FEATURE_USED("feature_used"),
    SETTINGS_CHANGED("settings_changed"),
    ERROR_OCCURRED("error_occurred"),
    THREAT_DETECTED("threat_detected"),
    REPORT_GENERATED("report_generated"),
    PRIVACY_RADAR_STARTED("privacy_radar_started"),
    PRIVACY_RADAR_STOPPED("privacy_radar_stopped"),
}

private const val BATCH_SIZE = 50
private val APP_VERSION: String = BuildConfig.VERSION_NAME
private const val MAX_EVENT_AGE_MS = 7L * 24 * 60 * 60 * 1000 // 7 days

@Singleton
class DefaultTelemetryRepository @Inject constructor(
    private val telemetryApi: TelemetryApi?,
    private val credentials: ApiCredentials,
    private val telemetryEventDao: TelemetryEventDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val json: Json,
) : TelemetryRepository {
    private val flushMutex = Mutex()

    override val isEnabled: Boolean
        get() = telemetryApi != null && credentials.isTelemetryConfigured

    private val deviceInfo: DeviceInfoDto by lazy {
        DeviceInfoDto(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            appVersion = APP_VERSION,
        )
    }

    override suspend fun trackEvent(
        eventType: TelemetryEventType,
        payload: Map<String, String>,
        sessionId: String?,
    ) {
        if (!isEnabled) return

        val event = TelemetryEventDto(
            sessionId = sessionId ?: UUID.randomUUID().toString(),
            eventType = eventType.value,
            timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            payload = payload.filterValues { it.isNotBlank() },
            appVersion = APP_VERSION,
            deviceInfo = deviceInfo,
        )
        val entity = TelemetryEventEntity(
            id = UUID.randomUUID().toString(),
            eventJson = json.encodeToString(TelemetryEventDto.serializer(), event),
            createdAtEpoch = System.currentTimeMillis(),
        )

        withContext(ioDispatcher) {
            telemetryEventDao.insert(entity)
            telemetryEventDao.deleteOlderThan(System.currentTimeMillis() - MAX_EVENT_AGE_MS)
        }
    }

    override suspend fun flush(): Result<Int> = flushMutex.withLock {
        if (!isEnabled) {
            return@withLock Result.success(0)
        }
        val api = telemetryApi ?: return@withLock Result.failure(
            IllegalStateException("Telemetry API not configured"),
        )

        val entities = withContext(ioDispatcher) {
            telemetryEventDao.fetchOldest(BATCH_SIZE)
        }

        if (entities.isEmpty()) return@withLock Result.success(0)

        val eventsToSend = entities.mapNotNull { entity ->
            runCatching { json.decodeFromString(TelemetryEventDto.serializer(), entity.eventJson) }.getOrNull()
        }

        return@withLock try {
            val batch = TelemetryBatchRequestDto(
                events = eventsToSend,
                batchId = UUID.randomUUID().toString(),
            )
            val response = api.sendBatchEvents(batch)

            if (response.status == "ok" || response.status == "success") {
                withContext(ioDispatcher) {
                    telemetryEventDao.deleteByIds(entities.map { it.id })
                }
                Result.success(response.processedEvents ?: eventsToSend.size)
            } else {
                Result.failure(RuntimeException(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun trackScanCompleted(
        sessionId: String,
        targetType: String,
        riskLevel: String,
        duration: Long,
    ) {
        trackEvent(
            eventType = TelemetryEventType.SCAN_COMPLETED,
            payload = mapOf(
                "target_type" to targetType,
                "risk_level" to riskLevel,
                "duration_ms" to duration.toString(),
            ),
            sessionId = sessionId,
        )
    }

    override suspend fun trackFeatureUsed(featureName: String) {
        trackEvent(
            eventType = TelemetryEventType.FEATURE_USED,
            payload = mapOf("feature" to featureName),
        )
    }

    override suspend fun trackError(errorType: String, context: String?) {
        trackEvent(
            eventType = TelemetryEventType.ERROR_OCCURRED,
            payload = buildMap {
                put("error_type", errorType)
                context?.let { put("context", it) }
            },
        )
    }
}

package com.v7lthronyx.scamynx.data.privacyradar.storage

import com.v7lthronyx.scamynx.data.db.PrivacyBaselineDao
import com.v7lthronyx.scamynx.data.db.PrivacyBaselineEntity
import com.v7lthronyx.scamynx.data.db.PrivacyEventDao
import com.v7lthronyx.scamynx.data.db.PrivacyEventEntity
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.di.ThreatIntelJson
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventMetadataKeys
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventType
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyResourceType
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyVisibilityContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class PrivacyBaselineRefresher @Inject constructor(
    private val privacyEventDao: PrivacyEventDao,
    private val privacyBaselineDao: PrivacyBaselineDao,
    @ThreatIntelJson private val json: Json,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val refreshRequests = MutableSharedFlow<BaselineKey>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val metadataSerializer = MapSerializer(String.serializer(), String.serializer())
    private val clock = Clock.System

    init {
        scope.launch {
            refreshRequests.collect { key ->
                recomputeBaseline(key)
            }
        }
    }

    fun track(event: PrivacyEvent) {
        val key = BaselineKey(
            packageName = event.packageName,
            resourceType = event.resourceType.name,
        )
        refreshRequests.tryEmit(key)
    }

    suspend fun refreshAll() = withContext(dispatcher) {
        val keys = privacyEventDao.getDistinctEventKeys()
        val nowMillis = clock.now().toEpochMilliseconds()
        keys.forEach { key ->
            recomputeBaseline(
                BaselineKey(
                    packageName = key.packageName,
                    resourceType = key.resourceType,
                ),
                nowMillisOverride = nowMillis,
            )
        }
    }

    suspend fun stop() {
        scope.coroutineContext.cancelChildren()
    }

    private suspend fun recomputeBaseline(
        key: BaselineKey,
        nowMillisOverride: Long? = null,
    ) {
        val nowMillis = nowMillisOverride ?: clock.now().toEpochMilliseconds()
        val windowStart = nowMillis - BASELINE_WINDOW_MS
        val events = privacyEventDao.getEventsForPackage(
            packageName = key.packageName,
            resourceType = key.resourceType,
            startEpochMillis = windowStart,
            endEpochMillis = nowMillis,
        )
        if (events.isEmpty()) {
            privacyBaselineDao.deleteBaseline(key.packageName, key.resourceType)
            return
        }
        val countsByDay = events
            .groupBy { it.timestampEpochMillis / MILLIS_PER_DAY }
            .values
            .map { it.size }
        val averageDailyCount = countsByDay.average().takeIf { it.isFinite() } ?: 0.0
        val stdDailyCount = countsByDay.standardDeviation(averageDailyCount)
        val medianDuration = events.medianDuration()
        val lastEvent = events.maxByOrNull { it.timestampEpochMillis }
        val visibilityCounts = events
            .groupingBy { it.visibilityContext }
            .eachCount()
        val expectedVisibility = visibilityCounts.maxByOrNull { it.value }?.key
        val baselineEntity = PrivacyBaselineEntity(
            packageName = key.packageName,
            resourceType = key.resourceType,
            averageDailyCount = averageDailyCount,
            standardDeviationDailyCount = stdDailyCount,
            medianDurationMs = medianDuration,
            lastSeenEpochMillis = lastEvent?.timestampEpochMillis,
            lastSeenForegroundState = lastEvent?.visibilityContext,
            expectedVisibility = expectedVisibility,
            permissionMismatchScore = computePermissionMismatchScore(events),
            overrideConflictScore = computeOverrideConflictScore(events),
            updatedAtEpochMillis = nowMillis,
        )
        privacyBaselineDao.upsert(baselineEntity)
    }

    private fun computePermissionMismatchScore(events: List<PrivacyEventEntity>): Double {
        val permissionSignals = events.filter { it.eventType == PrivacyEventType.PERMISSION_DELTA.name }
        if (permissionSignals.isEmpty()) return 0.0
        val accessSignals = events.count { it.eventType == PrivacyEventType.RESOURCE_ACCESS.name }
        val grants = permissionSignals.count { signal ->
            signal.metadata()[PrivacyEventMetadataKeys.GRANT_STATE]?.equals(VALUE_GRANTED, ignoreCase = true) == true
        }
        val revocations = permissionSignals.count { signal ->
            signal.metadata()[PrivacyEventMetadataKeys.GRANT_STATE]?.equals(VALUE_REVOKED, ignoreCase = true) == true
        }
        val base = when {
            grants == 0 -> 0.0
            accessSignals == 0 -> 1.0
            else -> ((grants - accessSignals).coerceAtLeast(0)).toDouble() / grants.toDouble()
        }
        val revocationPenalty = if (revocations > 0 && accessSignals > 0) 0.35 else 0.0
        val saturationPenalty = if (permissionSignals.size > accessSignals * 2) 0.15 else 0.0
        return (base + revocationPenalty + saturationPenalty).coerceIn(0.0, 1.0)
    }

    private fun computeOverrideConflictScore(events: List<PrivacyEventEntity>): Double {
        if (events.isEmpty()) return 0.0
        val highPriorityEvents = events.filter { it.resourceType in HIGH_PRIORITY_RESOURCES }
        val highPriorityCount = highPriorityEvents.size
        val backgroundHighPriority = highPriorityEvents.count {
            it.visibilityContext != PrivacyVisibilityContext.FOREGROUND.name
        }
        val metadataOverrideHits = events.count { event ->
            event.metadata()[PrivacyEventMetadataKeys.OVERRIDE_CONFLICT]?.equals("true", ignoreCase = true) == true
        }
        val sensorBlocks = events.filter {
            it.resourceType == PrivacyResourceType.SENSOR_PRIVACY_SWITCH.name &&
                it.metadata()[PrivacyEventMetadataKeys.SENSOR_PRIVACY_STATE]?.equals(VALUE_BLOCKED, ignoreCase = true) == true
        }.map { it.timestampEpochMillis }
        val blockedFollowUps = if (sensorBlocks.isEmpty() || highPriorityCount == 0) {
            0
        } else {
            highPriorityEvents.count { event ->
                sensorBlocks.any { blockTs -> event.timestampEpochMillis >= blockTs }
            }
        }
        val blockScore = if (sensorBlocks.isEmpty() || highPriorityCount == 0) {
            0.0
        } else {
            (blockedFollowUps.toDouble() / highPriorityCount.toDouble()).coerceIn(0.0, 1.0)
        }
        val backgroundScore = if (highPriorityCount == 0) {
            0.0
        } else {
            (backgroundHighPriority.toDouble() / highPriorityCount.toDouble()).coerceIn(0.0, 1.0)
        }
        val metadataScore = (metadataOverrideHits.toDouble() / events.size.toDouble()).coerceIn(0.0, 1.0)
        return maxOf(blockScore, backgroundScore, metadataScore)
    }

    private fun List<Int>.standardDeviation(mean: Double): Double {
        if (isEmpty() || size == 1) return 0.0
        val variance = map { value ->
            val delta = value - mean
            delta * delta
        }.average()
        return sqrt(variance)
    }

    private fun List<PrivacyEventEntity>.medianDuration(): Long? {
        val durations = mapNotNull { it.durationMs }.sorted()
        if (durations.isEmpty()) return null
        val middle = durations.size / 2
        return if (durations.size % 2 == 0) {
            (durations[middle - 1] + durations[middle]) / 2
        } else {
            durations[middle]
        }
    }

    private fun PrivacyEventEntity.metadata(): Map<String, String> {
        val payload = metadataJson ?: return emptyMap()
        return runCatching { json.decodeFromString(metadataSerializer, payload) }.getOrDefault(emptyMap())
    }

    private data class BaselineKey(
        val packageName: String,
        val resourceType: String,
    )

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val BASELINE_WINDOW_MS = 30L * MILLIS_PER_DAY
        private val HIGH_PRIORITY_RESOURCES = setOf(
            PrivacyResourceType.CAMERA.name,
            PrivacyResourceType.MICROPHONE.name,
            PrivacyResourceType.LOCATION.name,
            PrivacyResourceType.PHISHING_URL.name,
            PrivacyResourceType.WIFI_NETWORK.name,
        )
        private const val VALUE_GRANTED = "granted"
        private const val VALUE_REVOKED = "revoked"
        private const val VALUE_BLOCKED = "blocked"
    }
}

package com.v7lthronyx.scamynx.data.realtimeprotection

import com.v7lthronyx.scamynx.data.db.BlockedThreatEntity
import com.v7lthronyx.scamynx.domain.model.BlockAction
import com.v7lthronyx.scamynx.domain.model.BlockedThreat
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.ProtectionStatistics
import com.v7lthronyx.scamynx.domain.model.StatisticsPeriod
import com.v7lthronyx.scamynx.domain.model.ThreatSource
import com.v7lthronyx.scamynx.domain.model.ThreatType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedThreatsDao @Inject constructor(
    private val roomDao: BlockedThreatsRoomDao,
) {

    private val scanCount = AtomicInteger(0)

    suspend fun addThreat(threat: BlockedThreat) {
        roomDao.insertThreat(threat.toEntity())
    }

    suspend fun removeThreat(threatId: String) {
        roomDao.deleteThreat(threatId)
    }

    suspend fun getBlockedThreats(limit: Int, offset: Int): List<BlockedThreat> {
        return roomDao.getBlockedThreats(limit, offset).mapNotNull { it.toDomain() }
    }

    suspend fun getCountBySource(source: ThreatSource): Int {
        return roomDao.getCountBySource(source.name)
    }

    suspend fun getTodayCount(): Int {
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return roomDao.getCountSince(todayStart)
    }

    suspend fun getTotalCount(): Int = roomDao.getTotalCount()

    suspend fun getLastBlockedTime(): Instant? {
        return roomDao.getLastBlockedEpoch()?.let { Instant.fromEpochMilliseconds(it) }
    }

    fun getScanCount(): Int = scanCount.get()

    fun incrementScanCount() {
        scanCount.incrementAndGet()
    }

    suspend fun clearOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        roomDao.clearOlderThan(cutoff)
    }

    suspend fun getStatistics(period: StatisticsPeriod, now: Instant): ProtectionStatistics {
        val startTime = calculatePeriodStart(period, now)
        val threatsInPeriod = roomDao.getThreatsSince(startTime.toEpochMilliseconds())
            .mapNotNull { it.toDomain() }

        val threatsByType = threatsInPeriod
            .groupBy { it.threatType }
            .mapValues { it.value.size }

        val threatsBySeverity = threatsInPeriod
            .groupBy { it.severity }
            .mapValues { it.value.size }

        val threatsBySource = threatsInPeriod
            .groupBy { it.source }
            .mapValues { it.value.size }

        val topBlockedDomains = threatsInPeriod
            .filter { it.source in listOf(ThreatSource.BROWSER, ThreatSource.CLIPBOARD) }
            .groupBy { extractDomain(it.target) }
            .entries
            .sortedByDescending { it.value.size }
            .take(10)
            .map { it.key }

        val topBlockedApps = threatsInPeriod
            .filter { it.source == ThreatSource.APP || it.source == ThreatSource.NOTIFICATION }
            .mapNotNull { it.sourceApp }
            .groupBy { it }
            .entries
            .sortedByDescending { it.value.size }
            .take(10)
            .map { it.key }

        return ProtectionStatistics(
            period = period,
            threatsBlocked = threatsInPeriod.size,
            linksScanned = scanCount.get(),
            appsScanned = 0,
            notificationsScanned = 0,
            networkChecks = 0,
            threatsByType = threatsByType,
            threatsBySeverity = threatsBySeverity,
            threatsBySource = threatsBySource,
            topBlockedDomains = topBlockedDomains,
            topBlockedApps = topBlockedApps,
            protectionUptime = 100.0,
            generatedAt = now,
        )
    }

    private fun calculatePeriodStart(period: StatisticsPeriod, now: Instant): Instant {
        val tz = TimeZone.currentSystemDefault()
        return when (period) {
            StatisticsPeriod.TODAY -> now.minus(1, DateTimeUnit.DAY, tz)
            StatisticsPeriod.WEEK -> now.minus(7, DateTimeUnit.DAY, tz)
            StatisticsPeriod.MONTH -> now.minus(30, DateTimeUnit.DAY, tz)
            StatisticsPeriod.YEAR -> now.minus(365, DateTimeUnit.DAY, tz)
            StatisticsPeriod.ALL_TIME -> Instant.DISTANT_PAST
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            url.removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
                .substringBefore("?")
                .lowercase()
                .removePrefix("www.")
        } catch (e: Exception) {
            url
        }
    }
}

private fun BlockedThreat.toEntity(): BlockedThreatEntity = BlockedThreatEntity(
    id = id,
    threatType = threatType.name,
    source = source.name,
    target = target,
    severity = severity.name,
    action = action.name,
    reason = reason,
    blockedAtEpoch = blockedAt.toEpochMilliseconds(),
    sourceApp = sourceApp,
    userNotified = userNotified,
    canUnblock = canUnblock,
)

private fun BlockedThreatEntity.toDomain(): BlockedThreat? = try {
    BlockedThreat(
        id = id,
        threatType = ThreatType.valueOf(threatType),
        source = ThreatSource.valueOf(source),
        target = target,
        severity = IssueSeverity.valueOf(severity),
        action = BlockAction.valueOf(action),
        reason = reason,
        blockedAt = Instant.fromEpochMilliseconds(blockedAtEpoch),
        sourceApp = sourceApp,
        userNotified = userNotified,
        canUnblock = canUnblock,
    )
} catch (e: Exception) {
    null
}

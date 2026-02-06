package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.AppSecurityScan
import com.v7lthronyx.scamynx.domain.model.BlockedThreat
import com.v7lthronyx.scamynx.domain.model.LinkScanResult
import com.v7lthronyx.scamynx.domain.model.NetworkSecurityStatus
import com.v7lthronyx.scamynx.domain.model.NotificationScanResult
import com.v7lthronyx.scamynx.domain.model.ProtectionFeature
import com.v7lthronyx.scamynx.domain.model.ProtectionSettings
import com.v7lthronyx.scamynx.domain.model.ProtectionStatistics
import com.v7lthronyx.scamynx.domain.model.RealTimeProtectionState
import com.v7lthronyx.scamynx.domain.model.StatisticsPeriod
import com.v7lthronyx.scamynx.domain.model.ThreatSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RealTimeProtectionService {


    val protectionState: StateFlow<RealTimeProtectionState>

    suspend fun enableProtection()

    suspend fun disableProtection()

    suspend fun pauseProtection(durationMinutes: Int)

    suspend fun resumeProtection()

    suspend fun getProtectionFeatures(): List<ProtectionFeature>

    suspend fun toggleFeature(featureId: String, enabled: Boolean)


    suspend fun scanLink(
        url: String,
        source: ThreatSource,
        autoBlock: Boolean = true,
    ): LinkScanResult

    suspend fun batchScanLinks(
        urls: List<String>,
        source: ThreatSource,
    ): Map<String, LinkScanResult>

    fun observeLinkScans(): Flow<LinkScanResult>


    suspend fun scanApp(packageName: String): AppSecurityScan

    suspend fun scanAllApps(includeSystem: Boolean = false): List<AppSecurityScan>

    fun observeAppInstalls(): Flow<AppSecurityScan>

    suspend fun blockApp(packageName: String)

    suspend fun unblockApp(packageName: String)


    suspend fun getNetworkSecurityStatus(): NetworkSecurityStatus

    fun observeNetworkSecurity(): Flow<NetworkSecurityStatus>

    suspend fun startVpnProtection()

    suspend fun stopVpnProtection()

    suspend fun isVpnProtectionActive(): Boolean


    suspend fun scanNotification(
        packageName: String,
        title: String?,
        content: String?,
    ): NotificationScanResult

    fun observeNotificationScans(): Flow<NotificationScanResult>


    suspend fun scanClipboard(content: String): LinkScanResult?

    suspend fun enableClipboardMonitoring()

    suspend fun disableClipboardMonitoring()


    suspend fun getBlockedThreats(
        limit: Int = 50,
        offset: Int = 0,
    ): List<BlockedThreat>

    fun observeBlockedThreats(): Flow<BlockedThreat>

    suspend fun unblockThreat(threatId: String): Boolean

    suspend fun clearBlockedThreats(olderThanDays: Int = 30)


    suspend fun allowDomain(domain: String)

    suspend fun removeAllowedDomain(domain: String)

    suspend fun getAllowedDomains(): List<String>

    suspend fun blockDomain(domain: String)

    suspend fun removeBlockedDomain(domain: String)

    suspend fun getBlockedDomains(): List<String>


    suspend fun getStatistics(period: StatisticsPeriod): ProtectionStatistics

    suspend fun getTodayThreatCount(): Int

    suspend fun getTotalThreatsBlocked(): Int


    suspend fun getSettings(): ProtectionSettings

    suspend fun updateSettings(settings: ProtectionSettings)

    suspend fun resetSettings()
}

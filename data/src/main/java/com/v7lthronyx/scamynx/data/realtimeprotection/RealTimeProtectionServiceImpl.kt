package com.v7lthronyx.scamynx.data.realtimeprotection

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.AppPermissionRisk
import com.v7lthronyx.scamynx.domain.model.AppReputation
import com.v7lthronyx.scamynx.domain.model.AppSecurityScan
import com.v7lthronyx.scamynx.domain.model.AppThreat
import com.v7lthronyx.scamynx.domain.model.BatteryImpact
import com.v7lthronyx.scamynx.domain.model.BlockAction
import com.v7lthronyx.scamynx.domain.model.BlockedThreat
import com.v7lthronyx.scamynx.domain.model.CertificateInfo
import com.v7lthronyx.scamynx.domain.model.ConfidenceLevel
import com.v7lthronyx.scamynx.domain.model.ConnectionType
import com.v7lthronyx.scamynx.domain.model.DomainReputation
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.LinkScanResult
import com.v7lthronyx.scamynx.domain.model.NetworkSecurityStatus
import com.v7lthronyx.scamynx.domain.model.NetworkThreat
import com.v7lthronyx.scamynx.domain.model.NetworkThreatType
import com.v7lthronyx.scamynx.domain.model.NotificationScanResult
import com.v7lthronyx.scamynx.domain.model.PopularityLevel
import com.v7lthronyx.scamynx.domain.model.ProtectionFeature
import com.v7lthronyx.scamynx.domain.model.ProtectionSettings
import com.v7lthronyx.scamynx.domain.model.ProtectionStatistics
import com.v7lthronyx.scamynx.domain.model.ProtectionStatus
import com.v7lthronyx.scamynx.domain.model.RealTimeProtectionState
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.ScamIndicator
import com.v7lthronyx.scamynx.domain.model.StatisticsPeriod
import com.v7lthronyx.scamynx.domain.model.ThreatSource
import com.v7lthronyx.scamynx.domain.model.ThreatType
import com.v7lthronyx.scamynx.domain.model.WifiSecurityType
import com.v7lthronyx.scamynx.domain.service.RealTimeProtectionService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.net.URI
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of RealTimeProtectionService providing comprehensive real-time
 * protection against phishing, malware, and other threats.
 * 
 * Features:
 * - Link scanning with threat detection
 * - App security scanning
 * - Network security monitoring
 * - Notification scanning
 * - Clipboard monitoring
 * - Allowlist/Blocklist management
 */
@Singleton
class RealTimeProtectionServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val protectionPreferences: RealTimeProtectionPreferences,
    private val blockedThreatsDao: BlockedThreatsDao,
    private val linkAnalyzer: LinkThreatAnalyzer,
) : RealTimeProtectionService {

    private val serviceScope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _protectionState = MutableStateFlow(createInitialState())
    override val protectionState: StateFlow<RealTimeProtectionState> = _protectionState.asStateFlow()

    private val _linkScanFlow = MutableSharedFlow<LinkScanResult>(replay = 1)
    private val _appScanFlow = MutableSharedFlow<AppSecurityScan>(replay = 1)
    private val _notificationScanFlow = MutableSharedFlow<NotificationScanResult>(replay = 1)
    private val _blockedThreatFlow = MutableSharedFlow<BlockedThreat>(replay = 1)
    private val _networkStatusFlow = MutableSharedFlow<NetworkSecurityStatus>(replay = 1)

    private var pauseEndTime: Long? = null

    // region Protection Control

    override suspend fun enableProtection() = withContext(dispatcher) {
        protectionPreferences.setProtectionEnabled(true)
        pauseEndTime = null
        updateProtectionState()
    }

    override suspend fun disableProtection() = withContext(dispatcher) {
        protectionPreferences.setProtectionEnabled(false)
        updateProtectionState()
    }

    override suspend fun pauseProtection(durationMinutes: Int) = withContext(dispatcher) {
        pauseEndTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        updateProtectionState()
    }

    override suspend fun resumeProtection() = withContext(dispatcher) {
        pauseEndTime = null
        updateProtectionState()
    }

    override suspend fun getProtectionFeatures(): List<ProtectionFeature> = withContext(dispatcher) {
        val settings = protectionPreferences.getSettings()
        listOf(
            ProtectionFeature(
                id = "link_protection",
                name = "Link Protection",
                description = "Scans URLs for phishing and malware before opening",
                isEnabled = settings.linkProtection,
                isAvailable = true,
                blockedCount = blockedThreatsDao.getCountBySource(ThreatSource.BROWSER),
            ),
            ProtectionFeature(
                id = "app_protection",
                name = "App Protection",
                description = "Monitors installed apps for security threats",
                isEnabled = settings.appProtection,
                isAvailable = true,
                blockedCount = blockedThreatsDao.getCountBySource(ThreatSource.APP),
            ),
            ProtectionFeature(
                id = "network_protection",
                name = "Network Protection",
                description = "Monitors network connections for security issues",
                isEnabled = settings.networkProtection,
                isAvailable = true,
            ),
            ProtectionFeature(
                id = "notification_protection",
                name = "Notification Protection",
                description = "Scans notifications for suspicious content",
                isEnabled = settings.notificationProtection,
                requiresPermission = true,
                permissionGranted = hasNotificationListenerPermission(),
                blockedCount = blockedThreatsDao.getCountBySource(ThreatSource.NOTIFICATION),
            ),
            ProtectionFeature(
                id = "clipboard_protection",
                name = "Clipboard Protection",
                description = "Scans clipboard content for malicious URLs",
                isEnabled = settings.clipboardProtection,
                isAvailable = true,
                blockedCount = blockedThreatsDao.getCountBySource(ThreatSource.CLIPBOARD),
            ),
            ProtectionFeature(
                id = "qr_protection",
                name = "QR Code Protection",
                description = "Scans QR codes for malicious content",
                isEnabled = settings.qrCodeProtection,
                isAvailable = true,
                blockedCount = blockedThreatsDao.getCountBySource(ThreatSource.QR_CODE),
            ),
        )
    }

    override suspend fun toggleFeature(featureId: String, enabled: Boolean) = withContext(dispatcher) {
        val currentSettings = protectionPreferences.getSettings()
        val updatedSettings = when (featureId) {
            "link_protection" -> currentSettings.copy(linkProtection = enabled)
            "app_protection" -> currentSettings.copy(appProtection = enabled)
            "network_protection" -> currentSettings.copy(networkProtection = enabled)
            "notification_protection" -> currentSettings.copy(notificationProtection = enabled)
            "clipboard_protection" -> currentSettings.copy(clipboardProtection = enabled)
            "qr_protection" -> currentSettings.copy(qrCodeProtection = enabled)
            else -> currentSettings
        }
        protectionPreferences.updateSettings(updatedSettings)
        updateProtectionState()
    }

    // endregion

    // region Link Scanning

    override suspend fun scanLink(
        url: String,
        source: ThreatSource,
        autoBlock: Boolean,
    ): LinkScanResult = withContext(dispatcher) {
        val startTime = System.currentTimeMillis()
        val settings = protectionPreferences.getSettings()

        // Check allowlist first
        val domain = extractDomain(url)
        if (domain in settings.allowedDomains) {
            return@withContext createSafeLinkResult(url, domain, source, startTime)
        }

        // Check blocklist
        if (domain in settings.blockedDomains) {
            val result = createBlockedLinkResult(url, domain, source, startTime)
            recordBlockedThreat(result, source)
            _linkScanFlow.emit(result)
            return@withContext result
        }

        // Perform threat analysis
        val analysisResult = linkAnalyzer.analyze(url)

        val result = LinkScanResult(
            url = url,
            normalizedUrl = normalizeUrl(url),
            domain = domain,
            isSafe = analysisResult.isSafe,
            threatType = analysisResult.threatType,
            severity = analysisResult.severity,
            confidence = analysisResult.confidence,
            categories = analysisResult.categories,
            reputation = analysisResult.reputation,
            certificateInfo = null, // Would require HTTPS connection check
            redirectChain = emptyList(),
            scanDuration = System.currentTimeMillis() - startTime,
            scannedAt = clock.now(),
            source = source,
            action = if (!analysisResult.isSafe && autoBlock && settings.autoBlock) {
                BlockAction.BLOCKED
            } else if (!analysisResult.isSafe) {
                BlockAction.WARNED
            } else null,
        )

        if (!result.isSafe) {
            recordBlockedThreat(result, source)
        }

        _linkScanFlow.emit(result)
        blockedThreatsDao.incrementScanCount()
        result
    }

    override suspend fun batchScanLinks(
        urls: List<String>,
        source: ThreatSource,
    ): Map<String, LinkScanResult> = withContext(dispatcher) {
        urls.associateWith { url ->
            scanLink(url, source, autoBlock = true)
        }
    }

    override fun observeLinkScans(): Flow<LinkScanResult> = _linkScanFlow.asSharedFlow()

    // endregion

    // region App Scanning

    override suspend fun scanApp(packageName: String): AppSecurityScan = withContext(dispatcher) {
        val pm = context.packageManager
        
        try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(
                    PackageManager.GET_PERMISSIONS.toLong()
                ))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }

            val appName = pm.getApplicationLabel(appInfo).toString()
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            // Analyze permissions
            val permissionRisks = analyzeAppPermissions(packageInfo.requestedPermissions ?: arrayOf())
            
            // Determine risk level
            val highRiskCount = permissionRisks.count { it.riskLevel == RiskCategory.HIGH }
            val mediumRiskCount = permissionRisks.count { it.riskLevel == RiskCategory.MEDIUM }
            
            val riskLevel = when {
                highRiskCount >= 3 -> RiskCategory.HIGH
                highRiskCount >= 1 || mediumRiskCount >= 5 -> RiskCategory.MEDIUM
                mediumRiskCount >= 2 -> RiskCategory.LOW
                else -> RiskCategory.MINIMAL
            }

            val result = AppSecurityScan(
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                versionCode = versionCode,
                isSafe = riskLevel == RiskCategory.MINIMAL || riskLevel == RiskCategory.LOW,
                riskLevel = riskLevel,
                threats = emptyList(), // Would require deeper analysis
                permissions = permissionRisks,
                behaviors = emptyList(),
                reputation = AppReputation(
                    source = "PackageManager",
                    isDeveloperVerified = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                ),
                scannedAt = clock.now(),
            )

            _appScanFlow.emit(result)
            result
        } catch (e: PackageManager.NameNotFoundException) {
            AppSecurityScan(
                packageName = packageName,
                appName = "Unknown",
                versionName = "Unknown",
                versionCode = 0,
                isSafe = false,
                riskLevel = RiskCategory.CRITICAL,
                reputation = AppReputation(source = "Error"),
                scannedAt = clock.now(),
            )
        }
    }

    override suspend fun scanAllApps(includeSystem: Boolean): List<AppSecurityScan> =
        withContext(dispatcher) {
            val pm = context.packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }

            packages
                .filter { appInfo ->
                    includeSystem || (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .map { appInfo -> scanApp(appInfo.packageName) }
        }

    override fun observeAppInstalls(): Flow<AppSecurityScan> = _appScanFlow.asSharedFlow()

    override suspend fun blockApp(packageName: String) = withContext(dispatcher) {
        val settings = protectionPreferences.getSettings()
        val updatedAllowed = settings.allowedApps - packageName
        protectionPreferences.updateSettings(settings.copy(allowedApps = updatedAllowed))
    }

    override suspend fun unblockApp(packageName: String) = withContext(dispatcher) {
        val settings = protectionPreferences.getSettings()
        val updatedAllowed = settings.allowedApps + packageName
        protectionPreferences.updateSettings(settings.copy(allowedApps = updatedAllowed))
    }

    // endregion

    // region Network Security

    override suspend fun getNetworkSecurityStatus(): NetworkSecurityStatus = withContext(dispatcher) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }

        val connectionType = when {
            capabilities == null -> ConnectionType.NONE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> ConnectionType.VPN
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.NONE
        }

        val wifiInfo = if (connectionType == ConnectionType.WIFI) {
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo
        } else null

        val threats = mutableListOf<NetworkThreat>()
        var securityType: WifiSecurityType? = null

        if (connectionType == ConnectionType.WIFI && wifiInfo != null) {
            // Check for open WiFi
            @Suppress("DEPRECATION")
            val ssid = wifiInfo.ssid?.removeSurrounding("\"") ?: "Unknown"
            
            // This is a simplified check - real implementation would need more sophisticated detection
            if (!wifiManager.isWifiEnabled) {
                // WiFi is disabled
            } else {
                securityType = WifiSecurityType.WPA2 // Default assumption
            }
        }

        val status = NetworkSecurityStatus(
            isSecure = threats.isEmpty() && connectionType != ConnectionType.NONE,
            connectionType = connectionType,
            ssid = wifiInfo?.ssid?.removeSurrounding("\""),
            bssid = wifiInfo?.bssid,
            securityType = securityType,
            isVpnActive = connectionType == ConnectionType.VPN ||
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
            vpnProvider = null,
            threats = threats,
            dnsProvider = null,
            isDnsSecure = true,
            publicIp = null,
            geoLocation = null,
            checkedAt = clock.now(),
        )

        _networkStatusFlow.emit(status)
        status
    }

    override fun observeNetworkSecurity(): Flow<NetworkSecurityStatus> = _networkStatusFlow.asSharedFlow()

    override suspend fun startVpnProtection() {
        // VPN requires VpnService which needs its own Activity and permissions
        // This would typically launch the VPN connection dialog
    }

    override suspend fun stopVpnProtection() {
        // Stop VPN service
    }

    override suspend fun isVpnProtectionActive(): Boolean = withContext(dispatcher) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }

    // endregion

    // region Notification Scanning

    override suspend fun scanNotification(
        packageName: String,
        title: String?,
        content: String?,
    ): NotificationScanResult = withContext(dispatcher) {
        val pm = context.packageManager
        val appName = try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        // Extract URLs from notification content
        val fullText = "${title ?: ""} ${content ?: ""}"
        val extractedUrls = extractUrls(fullText)
        val extractedPhones = extractPhoneNumbers(fullText)

        // Analyze for scam indicators
        val indicators = mutableListOf<ScamIndicator>()
        var isSuspicious = false
        var threatType: ThreatType? = null
        var severity: IssueSeverity? = null

        // Check for urgency indicators
        val urgencyPatterns = listOf(
            "urgent", "immediately", "act now", "limited time", "expires",
            "account suspended", "verify now", "click here", "confirm your"
        )
        val textLower = fullText.lowercase()
        
        if (urgencyPatterns.any { textLower.contains(it) }) {
            indicators.add(ScamIndicator.URGENCY)
            isSuspicious = true
        }

        // Check for suspicious URLs
        for (url in extractedUrls) {
            val linkResult = linkAnalyzer.analyze(url)
            if (!linkResult.isSafe) {
                isSuspicious = true
                threatType = linkResult.threatType
                severity = linkResult.severity
                break
            }
        }

        val result = NotificationScanResult(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appName = appName,
            title = title,
            content = content,
            isSuspicious = isSuspicious,
            threatType = threatType,
            severity = severity,
            extractedUrls = extractedUrls,
            extractedPhones = extractedPhones,
            indicators = indicators,
            action = if (isSuspicious) BlockAction.WARNED else null,
            scannedAt = clock.now(),
        )

        if (isSuspicious) {
            recordNotificationThreat(result)
        }

        _notificationScanFlow.emit(result)
        result
    }

    override fun observeNotificationScans(): Flow<NotificationScanResult> = 
        _notificationScanFlow.asSharedFlow()

    // endregion

    // region Clipboard

    override suspend fun scanClipboard(content: String): LinkScanResult? = withContext(dispatcher) {
        val urls = extractUrls(content)
        if (urls.isEmpty()) return@withContext null

        scanLink(urls.first(), ThreatSource.CLIPBOARD, autoBlock = false)
    }

    override suspend fun enableClipboardMonitoring() = withContext(dispatcher) {
        val settings = protectionPreferences.getSettings()
        protectionPreferences.updateSettings(settings.copy(clipboardProtection = true))
    }

    override suspend fun disableClipboardMonitoring() = withContext(dispatcher) {
        val settings = protectionPreferences.getSettings()
        protectionPreferences.updateSettings(settings.copy(clipboardProtection = false))
    }

    // endregion

    // region Blocked Threats

    override suspend fun getBlockedThreats(
        limit: Int,
        offset: Int,
    ): List<BlockedThreat> = withContext(dispatcher) {
        blockedThreatsDao.getBlockedThreats(limit, offset)
    }

    override fun observeBlockedThreats(): Flow<BlockedThreat> = _blockedThreatFlow.asSharedFlow()

    override suspend fun unblockThreat(threatId: String): Boolean = withContext(dispatcher) {
        blockedThreatsDao.removeThreat(threatId)
        true
    }

    override suspend fun clearBlockedThreats(olderThanDays: Int) = withContext(dispatcher) {
        blockedThreatsDao.clearOlderThan(olderThanDays)
    }

    // endregion

    // region Domain Allowlist/Blocklist

    override suspend fun allowDomain(domain: String) = withContext(dispatcher) {
        val settings = protectionPreferences.getSettings()
        val updatedAllowed = settings.allowedDomains + domain
        val updatedBlocked = settings.blockedDomains - domain
        protectionPreferences.updateSettings(
            settings.copy(
                allowedDomains = updatedAllowed,
                blockedDomains = updatedBlocked
            )
        )
    }

    override suspend fun removeAllowedDomain(domain: String) = withContext(dispatcher) {
        val settings = protectionPreferences.getSettings()
        val updated = settings.allowedDomains - domain
        protectionPreferences.updateSettings(settings.copy(allowedDomains = updated))
    }

    override suspend fun getAllowedDomains(): List<String> = withContext(dispatcher) {
        protectionPreferences.getSettings().allowedDomains
    }

    override suspend fun blockDomain(domain: String) = withContext(dispatcher) {
        val settings = protectionPreferences.getSettings()
        val updatedBlocked = settings.blockedDomains + domain
        val updatedAllowed = settings.allowedDomains - domain
        protectionPreferences.updateSettings(
            settings.copy(
                blockedDomains = updatedBlocked,
                allowedDomains = updatedAllowed
            )
        )
    }

    override suspend fun removeBlockedDomain(domain: String) = withContext(dispatcher) {
        val settings = protectionPreferences.getSettings()
        val updated = settings.blockedDomains - domain
        protectionPreferences.updateSettings(settings.copy(blockedDomains = updated))
    }

    override suspend fun getBlockedDomains(): List<String> = withContext(dispatcher) {
        protectionPreferences.getSettings().blockedDomains
    }

    // endregion

    // region Statistics

    override suspend fun getStatistics(period: StatisticsPeriod): ProtectionStatistics =
        withContext(dispatcher) {
            blockedThreatsDao.getStatistics(period, clock.now())
        }

    override suspend fun getTodayThreatCount(): Int = withContext(dispatcher) {
        blockedThreatsDao.getTodayCount()
    }

    override suspend fun getTotalThreatsBlocked(): Int = withContext(dispatcher) {
        blockedThreatsDao.getTotalCount()
    }

    // endregion

    // region Settings

    override suspend fun getSettings(): ProtectionSettings = withContext(dispatcher) {
        protectionPreferences.getSettings()
    }

    override suspend fun updateSettings(settings: ProtectionSettings) = withContext(dispatcher) {
        protectionPreferences.updateSettings(settings)
        updateProtectionState()
    }

    override suspend fun resetSettings() = withContext(dispatcher) {
        protectionPreferences.resetToDefaults()
        updateProtectionState()
    }

    // endregion

    // region Private Helpers

    private fun createInitialState(): RealTimeProtectionState {
        return RealTimeProtectionState(
            overallStatus = ProtectionStatus.DISABLED,
            features = emptyList(),
            threatsBlockedToday = 0,
            threatsBlockedTotal = 0,
            lastThreatBlocked = null,
            scansSinceLastReboot = 0,
            activeMonitors = 0,
            batteryImpact = BatteryImpact.MINIMAL,
            lastUpdated = clock.now(),
        )
    }

    private suspend fun updateProtectionState() {
        val settings = protectionPreferences.getSettings()
        val features = getProtectionFeatures()
        val todayCount = blockedThreatsDao.getTodayCount()
        val totalCount = blockedThreatsDao.getTotalCount()

        val isPaused = pauseEndTime?.let { System.currentTimeMillis() < it } ?: false
        
        val status = when {
            !settings.isEnabled -> ProtectionStatus.DISABLED
            isPaused -> ProtectionStatus.PAUSED
            features.all { it.isEnabled } -> ProtectionStatus.ACTIVE
            features.any { it.isEnabled } -> ProtectionStatus.PARTIAL
            else -> ProtectionStatus.DISABLED
        }

        val activeMonitors = features.count { it.isEnabled && it.isAvailable }

        _protectionState.value = RealTimeProtectionState(
            overallStatus = status,
            features = features,
            threatsBlockedToday = todayCount,
            threatsBlockedTotal = totalCount,
            lastThreatBlocked = blockedThreatsDao.getLastBlockedTime(),
            scansSinceLastReboot = blockedThreatsDao.getScanCount(),
            activeMonitors = activeMonitors,
            batteryImpact = calculateBatteryImpact(activeMonitors),
            lastUpdated = clock.now(),
        )
    }

    private fun calculateBatteryImpact(activeMonitors: Int): BatteryImpact {
        return when {
            activeMonitors >= 5 -> BatteryImpact.MODERATE
            activeMonitors >= 3 -> BatteryImpact.LOW
            else -> BatteryImpact.MINIMAL
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            URI(url).host?.lowercase()?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url.removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
                .substringBefore("?")
                .lowercase()
                .removePrefix("www.")
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.trim().lowercase()
    }

    private fun createSafeLinkResult(
        url: String,
        domain: String,
        source: ThreatSource,
        startTime: Long,
    ): LinkScanResult {
        return LinkScanResult(
            url = url,
            normalizedUrl = normalizeUrl(url),
            domain = domain,
            isSafe = true,
            confidence = ConfidenceLevel.HIGH,
            categories = emptyList(),
            reputation = DomainReputation(score = 100),
            scanDuration = System.currentTimeMillis() - startTime,
            scannedAt = clock.now(),
            source = source,
        )
    }

    private fun createBlockedLinkResult(
        url: String,
        domain: String,
        source: ThreatSource,
        startTime: Long,
    ): LinkScanResult {
        return LinkScanResult(
            url = url,
            normalizedUrl = normalizeUrl(url),
            domain = domain,
            isSafe = false,
            threatType = ThreatType.PHISHING,
            severity = IssueSeverity.HIGH,
            confidence = ConfidenceLevel.HIGH,
            categories = listOf("blocked"),
            reputation = DomainReputation(score = 0),
            scanDuration = System.currentTimeMillis() - startTime,
            scannedAt = clock.now(),
            source = source,
            action = BlockAction.BLOCKED,
        )
    }

    private suspend fun recordBlockedThreat(result: LinkScanResult, source: ThreatSource) {
        val threat = BlockedThreat(
            id = UUID.randomUUID().toString(),
            threatType = result.threatType ?: ThreatType.PHISHING,
            source = source,
            target = result.url,
            severity = result.severity ?: IssueSeverity.MEDIUM,
            action = result.action ?: BlockAction.WARNED,
            reason = "Threat detected during scan",
            blockedAt = clock.now(),
            userNotified = true,
            canUnblock = true,
        )
        blockedThreatsDao.addThreat(threat)
        _blockedThreatFlow.emit(threat)
    }

    private suspend fun recordNotificationThreat(result: NotificationScanResult) {
        val threat = BlockedThreat(
            id = UUID.randomUUID().toString(),
            threatType = result.threatType ?: ThreatType.SOCIAL_ENGINEERING,
            source = ThreatSource.NOTIFICATION,
            target = "${result.appName}: ${result.title ?: "Notification"}",
            severity = result.severity ?: IssueSeverity.MEDIUM,
            action = result.action ?: BlockAction.WARNED,
            reason = "Suspicious notification content detected",
            blockedAt = clock.now(),
            sourceApp = result.packageName,
            userNotified = true,
            canUnblock = false,
        )
        blockedThreatsDao.addThreat(threat)
        _blockedThreatFlow.emit(threat)
    }

    private fun hasNotificationListenerPermission(): Boolean {
        val packageName = context.packageName
        val flat = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(packageName) == true
    }

    private fun analyzeAppPermissions(permissions: Array<String>): List<AppPermissionRisk> {
        val dangerousPermissions = mapOf(
            "android.permission.READ_CONTACTS" to Pair(RiskCategory.MEDIUM, "Access to contacts"),
            "android.permission.READ_CALL_LOG" to Pair(RiskCategory.HIGH, "Access to call history"),
            "android.permission.READ_SMS" to Pair(RiskCategory.HIGH, "Access to SMS messages"),
            "android.permission.SEND_SMS" to Pair(RiskCategory.HIGH, "Can send SMS messages"),
            "android.permission.CAMERA" to Pair(RiskCategory.MEDIUM, "Access to camera"),
            "android.permission.RECORD_AUDIO" to Pair(RiskCategory.HIGH, "Access to microphone"),
            "android.permission.ACCESS_FINE_LOCATION" to Pair(RiskCategory.MEDIUM, "Precise location access"),
            "android.permission.READ_EXTERNAL_STORAGE" to Pair(RiskCategory.MEDIUM, "Access to storage"),
            "android.permission.WRITE_EXTERNAL_STORAGE" to Pair(RiskCategory.MEDIUM, "Can modify storage"),
            "android.permission.SYSTEM_ALERT_WINDOW" to Pair(RiskCategory.HIGH, "Can draw over other apps"),
            "android.permission.BIND_ACCESSIBILITY_SERVICE" to Pair(RiskCategory.CRITICAL, "Accessibility service"),
            "android.permission.BIND_DEVICE_ADMIN" to Pair(RiskCategory.CRITICAL, "Device admin access"),
        )

        return permissions.mapNotNull { permission ->
            dangerousPermissions[permission]?.let { (risk, reason) ->
                AppPermissionRisk(
                    permission = permission,
                    riskLevel = risk,
                    reason = reason,
                    isGranted = true, // Would need to check actual grant status
                    isRequired = false,
                )
            }
        }
    }

    private val urlPattern = Pattern.compile(
        "https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?",
        Pattern.CASE_INSENSITIVE
    )

    private fun extractUrls(text: String): List<String> {
        val matcher = urlPattern.matcher(text)
        val urls = mutableListOf<String>()
        while (matcher.find()) {
            urls.add(matcher.group())
        }
        return urls
    }

    private val phonePattern = Pattern.compile(
        "\\+?[0-9][0-9\\s\\-().]{6,20}[0-9]"
    )

    private fun extractPhoneNumbers(text: String): List<String> {
        val matcher = phonePattern.matcher(text)
        val phones = mutableListOf<String>()
        while (matcher.find()) {
            phones.add(matcher.group())
        }
        return phones
    }

    // endregion
}

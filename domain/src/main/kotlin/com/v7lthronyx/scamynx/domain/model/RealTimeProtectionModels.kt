package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class ProtectionStatus {
    @SerialName("active")
    ACTIVE,

    @SerialName("partial")
    PARTIAL,

    @SerialName("paused")
    PAUSED,

    @SerialName("disabled")
    DISABLED,

    @SerialName("error")
    ERROR,
}

@Serializable
data class ProtectionFeature(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val isAvailable: Boolean = true,
    val requiresPermission: Boolean = false,
    val permissionGranted: Boolean = true,
    val lastTriggered: Instant? = null,
    val blockedCount: Int = 0,
    val isPremium: Boolean = false,
)

@Serializable
data class RealTimeProtectionState(
    val overallStatus: ProtectionStatus,
    val features: List<ProtectionFeature>,
    val threatsBlockedToday: Int,
    val threatsBlockedTotal: Int,
    val lastThreatBlocked: Instant? = null,
    val scansSinceLastReboot: Int,
    val activeMonitors: Int,
    val batteryImpact: BatteryImpact,
    val lastUpdated: Instant,
)

@Serializable
enum class BatteryImpact {
    @SerialName("minimal")
    MINIMAL,

    @SerialName("low")
    LOW,

    @SerialName("moderate")
    MODERATE,

    @SerialName("high")
    HIGH,
}


@Serializable
data class BlockedThreat(
    val id: String,
    val threatType: ThreatType,
    val source: ThreatSource,
    val target: String,
    val severity: IssueSeverity,
    val action: BlockAction,
    val reason: String,
    val blockedAt: Instant,
    val sourceApp: String? = null,
    val userNotified: Boolean = true,
    val canUnblock: Boolean = false,
)

@Serializable
enum class ThreatSource {
    @SerialName("browser")
    BROWSER,

    @SerialName("app")
    APP,

    @SerialName("notification")
    NOTIFICATION,

    @SerialName("clipboard")
    CLIPBOARD,

    @SerialName("qr_code")
    QR_CODE,

    @SerialName("sms")
    SMS,

    @SerialName("email")
    EMAIL,

    @SerialName("download")
    DOWNLOAD,

    @SerialName("network")
    NETWORK,

    @SerialName("system")
    SYSTEM,
}

@Serializable
enum class BlockAction {
    @SerialName("blocked")
    BLOCKED,

    @SerialName("warned")
    WARNED,

    @SerialName("quarantined")
    QUARANTINED,

    @SerialName("deleted")
    DELETED,

    @SerialName("reported")
    REPORTED,
}


@Serializable
data class LinkScanResult(
    val url: String,
    val normalizedUrl: String,
    val domain: String,
    val isSafe: Boolean,
    val threatType: ThreatType? = null,
    val severity: IssueSeverity? = null,
    val confidence: ConfidenceLevel,
    val categories: List<String> = emptyList(),
    val reputation: DomainReputation,
    val certificateInfo: CertificateInfo? = null,
    val redirectChain: List<String> = emptyList(),
    val scanDuration: Long,
    val scannedAt: Instant,
    val source: ThreatSource,
    val action: BlockAction? = null,
)

@Serializable
data class DomainReputation(
    val score: Int,
    val age: String? = null,
    val registrar: String? = null,
    val isNewlyRegistered: Boolean = false,
    val hasValidSsl: Boolean = true,
    val isParked: Boolean = false,
    val popularity: PopularityLevel = PopularityLevel.UNKNOWN,
)

@Serializable
enum class PopularityLevel {
    @SerialName("very_popular")
    VERY_POPULAR,

    @SerialName("popular")
    POPULAR,

    @SerialName("moderate")
    MODERATE,

    @SerialName("low")
    LOW,

    @SerialName("very_low")
    VERY_LOW,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
data class CertificateInfo(
    val issuer: String,
    val subject: String,
    val validFrom: Instant,
    val validTo: Instant,
    val isValid: Boolean,
    val isExpired: Boolean,
    val isSelfSigned: Boolean,
    val keyStrength: Int,
    val signatureAlgorithm: String,
)


@Serializable
data class AppSecurityScan(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSafe: Boolean,
    val riskLevel: RiskCategory,
    val threats: List<AppThreat> = emptyList(),
    val permissions: List<AppPermissionRisk> = emptyList(),
    val behaviors: List<SuspiciousBehavior> = emptyList(),
    val reputation: AppReputation,
    val scannedAt: Instant,
)

@Serializable
data class AppThreat(
    val type: ThreatType,
    val severity: IssueSeverity,
    val description: String,
    val evidence: String? = null,
)

@Serializable
data class AppPermissionRisk(
    val permission: String,
    val riskLevel: RiskCategory,
    val reason: String,
    val isGranted: Boolean,
    val isRequired: Boolean,
)

@Serializable
data class SuspiciousBehavior(
    val behavior: String,
    val description: String,
    val severity: IssueSeverity,
    val detectedAt: Instant? = null,
)

@Serializable
data class AppReputation(
    val source: String,
    val rating: Double? = null,
    val downloads: String? = null,
    val developer: String? = null,
    val isDeveloperVerified: Boolean = false,
    val hasKnownIssues: Boolean = false,
)


@Serializable
data class NetworkSecurityStatus(
    val isSecure: Boolean,
    val connectionType: ConnectionType,
    val ssid: String? = null,
    val bssid: String? = null,
    val securityType: WifiSecurityType? = null,
    val isVpnActive: Boolean,
    val vpnProvider: String? = null,
    val threats: List<NetworkThreat> = emptyList(),
    val dnsProvider: String? = null,
    val isDnsSecure: Boolean = true,
    val publicIp: String? = null,
    val geoLocation: GeoAttribution? = null,
    val checkedAt: Instant,
)

@Serializable
enum class ConnectionType {
    @SerialName("wifi")
    WIFI,

    @SerialName("cellular")
    CELLULAR,

    @SerialName("ethernet")
    ETHERNET,

    @SerialName("vpn")
    VPN,

    @SerialName("none")
    NONE,
}

@Serializable
enum class WifiSecurityType {
    @SerialName("open")
    OPEN,

    @SerialName("wep")
    WEP,

    @SerialName("wpa")
    WPA,

    @SerialName("wpa2")
    WPA2,

    @SerialName("wpa3")
    WPA3,

    @SerialName("enterprise")
    ENTERPRISE,
}

@Serializable
data class NetworkThreat(
    val type: NetworkThreatType,
    val severity: IssueSeverity,
    val description: String,
    val recommendation: String,
)

@Serializable
enum class NetworkThreatType {
    @SerialName("unsecured_wifi")
    UNSECURED_WIFI,

    @SerialName("evil_twin")
    EVIL_TWIN,

    @SerialName("arp_spoofing")
    ARP_SPOOFING,

    @SerialName("dns_hijacking")
    DNS_HIJACKING,

    @SerialName("ssl_stripping")
    SSL_STRIPPING,

    @SerialName("mitm")
    MITM,

    @SerialName("rogue_ap")
    ROGUE_AP,

    @SerialName("captive_portal")
    CAPTIVE_PORTAL,
}


@Serializable
data class NotificationScanResult(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val content: String?,
    val isSuspicious: Boolean,
    val threatType: ThreatType? = null,
    val severity: IssueSeverity? = null,
    val extractedUrls: List<String> = emptyList(),
    val extractedPhones: List<String> = emptyList(),
    val indicators: List<ScamIndicator> = emptyList(),
    val action: BlockAction? = null,
    val scannedAt: Instant,
)


@Serializable
data class ProtectionStatistics(
    val period: StatisticsPeriod,
    val threatsBlocked: Int,
    val linksScanned: Int,
    val appsScanned: Int,
    val notificationsScanned: Int,
    val networkChecks: Int,
    val threatsByType: Map<ThreatType, Int>,
    val threatsBySeverity: Map<IssueSeverity, Int>,
    val threatsBySource: Map<ThreatSource, Int>,
    val topBlockedDomains: List<String>,
    val topBlockedApps: List<String>,
    val protectionUptime: Double,
    val generatedAt: Instant,
)

@Serializable
enum class StatisticsPeriod {
    @SerialName("today")
    TODAY,

    @SerialName("week")
    WEEK,

    @SerialName("month")
    MONTH,

    @SerialName("year")
    YEAR,

    @SerialName("all_time")
    ALL_TIME,
}


@Serializable
data class ProtectionSettings(
    val isEnabled: Boolean = true,
    val linkProtection: Boolean = true,
    val appProtection: Boolean = true,
    val networkProtection: Boolean = true,
    val notificationProtection: Boolean = true,
    val clipboardProtection: Boolean = true,
    val qrCodeProtection: Boolean = true,
    val autoBlock: Boolean = true,
    val notifyOnBlock: Boolean = true,
    val notifyOnWarning: Boolean = true,
    val blockSeverityThreshold: IssueSeverity = IssueSeverity.MEDIUM,
    val scanOnWifiOnly: Boolean = false,
    val lowPowerMode: Boolean = false,
    val allowedDomains: List<String> = emptyList(),
    val blockedDomains: List<String> = emptyList(),
    val allowedApps: List<String> = emptyList(),
)

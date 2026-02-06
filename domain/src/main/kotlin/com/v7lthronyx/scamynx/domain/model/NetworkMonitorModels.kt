package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class NetworkMonitorStatus {
    @SerialName("active")
    ACTIVE,

    @SerialName("paused")
    PAUSED,

    @SerialName("disabled")
    DISABLED,

    @SerialName("no_permission")
    NO_PERMISSION,

    @SerialName("error")
    ERROR,
}

@Serializable
data class NetworkMonitorState(
    val status: NetworkMonitorStatus,
    val isVpnModeActive: Boolean,
    val connectionsMonitored: Int,
    val suspiciousConnections: Int,
    val blockedConnections: Int,
    val dataUsageToday: DataUsage,
    val activeConnections: List<ActiveConnection>,
    val recentAlerts: List<NetworkAlert>,
    val topApps: List<AppNetworkUsage>,
    val lastUpdated: Instant,
)

@Serializable
data class DataUsage(
    val sent: Long,
    val received: Long,
    val total: Long,
    val formattedSent: String,
    val formattedReceived: String,
    val formattedTotal: String,
)


@Serializable
data class ActiveConnection(
    val id: String,
    val packageName: String,
    val appName: String,
    val protocol: NetworkProtocol,
    val localAddress: String,
    val localPort: Int,
    val remoteAddress: String,
    val remotePort: Int,
    val remoteDomain: String? = null,
    val state: ConnectionState,
    val direction: ConnectionDirection,
    val bytesSent: Long,
    val bytesReceived: Long,
    val startTime: Instant,
    val geoLocation: GeoAttribution? = null,
    val isSuspicious: Boolean = false,
    val threatLevel: RiskCategory = RiskCategory.MINIMAL,
)

@Serializable
enum class NetworkProtocol {
    @SerialName("tcp")
    TCP,

    @SerialName("udp")
    UDP,

    @SerialName("icmp")
    ICMP,

    @SerialName("other")
    OTHER,
}

@Serializable
enum class ConnectionState {
    @SerialName("established")
    ESTABLISHED,

    @SerialName("listen")
    LISTEN,

    @SerialName("syn_sent")
    SYN_SENT,

    @SerialName("syn_received")
    SYN_RECEIVED,

    @SerialName("fin_wait")
    FIN_WAIT,

    @SerialName("close_wait")
    CLOSE_WAIT,

    @SerialName("closed")
    CLOSED,

    @SerialName("time_wait")
    TIME_WAIT,
}

@Serializable
enum class ConnectionDirection {
    @SerialName("inbound")
    INBOUND,

    @SerialName("outbound")
    OUTBOUND,
}


@Serializable
data class NetworkAlert(
    val id: String,
    val type: NetworkAlertType,
    val severity: IssueSeverity,
    val title: String,
    val description: String,
    val packageName: String? = null,
    val appName: String? = null,
    val remoteHost: String? = null,
    val remoteIp: String? = null,
    val action: NetworkAlertAction,
    val timestamp: Instant,
    val isAcknowledged: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class NetworkAlertType {
    @SerialName("suspicious_connection")
    SUSPICIOUS_CONNECTION,

    @SerialName("known_malware_server")
    KNOWN_MALWARE_SERVER,

    @SerialName("data_exfiltration")
    DATA_EXFILTRATION,

    @SerialName("unusual_traffic")
    UNUSUAL_TRAFFIC,

    @SerialName("unencrypted_sensitive")
    UNENCRYPTED_SENSITIVE,

    @SerialName("dns_tunneling")
    DNS_TUNNELING,

    @SerialName("port_scan")
    PORT_SCAN,

    @SerialName("tor_connection")
    TOR_CONNECTION,

    @SerialName("vpn_leak")
    VPN_LEAK,

    @SerialName("crypto_mining")
    CRYPTO_MINING,

    @SerialName("botnet_activity")
    BOTNET_ACTIVITY,

    @SerialName("c2_communication")
    C2_COMMUNICATION,

    @SerialName("ad_tracker")
    AD_TRACKER,

    @SerialName("analytics_tracker")
    ANALYTICS_TRACKER,

    @SerialName("background_data")
    BACKGROUND_DATA,
}

@Serializable
enum class NetworkAlertAction {
    @SerialName("blocked")
    BLOCKED,

    @SerialName("warned")
    WARNED,

    @SerialName("allowed")
    ALLOWED,

    @SerialName("logged")
    LOGGED,
}


@Serializable
data class AppNetworkUsage(
    val packageName: String,
    val appName: String,
    val appIcon: String? = null,
    val dataUsage: DataUsage,
    val connectionCount: Int,
    val suspiciousConnections: Int,
    val blockedConnections: Int,
    val uniqueHosts: Int,
    val topHosts: List<HostUsage>,
    val lastActivity: Instant,
    val trustLevel: AppTrustLevel,
)

@Serializable
data class HostUsage(
    val host: String,
    val ip: String? = null,
    val connectionCount: Int,
    val dataUsage: DataUsage,
    val category: HostCategory,
    val isSuspicious: Boolean = false,
)

@Serializable
enum class HostCategory {
    @SerialName("app_server")
    APP_SERVER,

    @SerialName("cdn")
    CDN,

    @SerialName("analytics")
    ANALYTICS,

    @SerialName("advertising")
    ADVERTISING,

    @SerialName("social_media")
    SOCIAL_MEDIA,

    @SerialName("cloud_service")
    CLOUD_SERVICE,

    @SerialName("gaming")
    GAMING,

    @SerialName("streaming")
    STREAMING,

    @SerialName("unknown")
    UNKNOWN,

    @SerialName("suspicious")
    SUSPICIOUS,

    @SerialName("malicious")
    MALICIOUS,
}

@Serializable
enum class AppTrustLevel {
    @SerialName("system")
    SYSTEM,

    @SerialName("verified")
    VERIFIED,

    @SerialName("trusted")
    TRUSTED,

    @SerialName("neutral")
    NEUTRAL,

    @SerialName("suspicious")
    SUSPICIOUS,

    @SerialName("blocked")
    BLOCKED,
}


@Serializable
data class DnsQuery(
    val id: String,
    val domain: String,
    val queryType: DnsQueryType,
    val resolvedIps: List<String>,
    val responseCode: DnsResponseCode,
    val packageName: String? = null,
    val appName: String? = null,
    val latency: Long,
    val isCached: Boolean,
    val isBlocked: Boolean,
    val blockReason: String? = null,
    val timestamp: Instant,
)

@Serializable
enum class DnsQueryType {
    @SerialName("a")
    A,

    @SerialName("aaaa")
    AAAA,

    @SerialName("cname")
    CNAME,

    @SerialName("mx")
    MX,

    @SerialName("txt")
    TXT,

    @SerialName("srv")
    SRV,

    @SerialName("ptr")
    PTR,

    @SerialName("other")
    OTHER,
}

@Serializable
enum class DnsResponseCode {
    @SerialName("no_error")
    NO_ERROR,

    @SerialName("nxdomain")
    NXDOMAIN,

    @SerialName("servfail")
    SERVFAIL,

    @SerialName("refused")
    REFUSED,

    @SerialName("timeout")
    TIMEOUT,

    @SerialName("blocked")
    BLOCKED,
}

@Serializable
data class DnsStatistics(
    val totalQueries: Int,
    val blockedQueries: Int,
    val uniqueDomains: Int,
    val averageLatency: Long,
    val queriesByType: Map<DnsQueryType, Int>,
    val topDomains: List<DomainStats>,
    val topBlockedDomains: List<DomainStats>,
    val period: StatisticsPeriod,
    val generatedAt: Instant,
)

@Serializable
data class DomainStats(
    val domain: String,
    val queryCount: Int,
    val category: HostCategory,
    val isBlocked: Boolean,
)


@Serializable
data class FirewallRule(
    val id: String,
    val name: String,
    val description: String? = null,
    val isEnabled: Boolean,
    val action: FirewallAction,
    val direction: ConnectionDirection? = null,
    val protocol: NetworkProtocol? = null,
    val packageName: String? = null,
    val remoteHost: String? = null,
    val remotePort: Int? = null,
    val localPort: Int? = null,
    val priority: Int,
    val hitCount: Int = 0,
    val lastHit: Instant? = null,
    val createdAt: Instant,
    val isSystem: Boolean = false,
)

@Serializable
enum class FirewallAction {
    @SerialName("allow")
    ALLOW,

    @SerialName("block")
    BLOCK,

    @SerialName("log")
    LOG,
}

@Serializable
data class FirewallConfig(
    val isEnabled: Boolean,
    val defaultAction: FirewallAction,
    val blockOnCellular: Boolean,
    val blockOnRoaming: Boolean,
    val allowSystemApps: Boolean,
    val blockAds: Boolean,
    val blockTrackers: Boolean,
    val blockMalware: Boolean,
    val customRules: List<FirewallRule>,
    val whitelistedApps: List<String>,
    val blacklistedApps: List<String>,
)


@Serializable
data class NetworkInsights(
    val anomalies: List<NetworkAnomaly>,
    val privacyConcerns: List<PrivacyConcern>,
    val recommendations: List<NetworkRecommendation>,
    val riskScore: Double,
    val analysisTimestamp: Instant,
)

@Serializable
data class NetworkAnomaly(
    val type: AnomalyType,
    val severity: IssueSeverity,
    val description: String,
    val affectedApp: String? = null,
    val detectedAt: Instant,
    val evidence: String? = null,
)

@Serializable
enum class AnomalyType {
    @SerialName("unusual_data_volume")
    UNUSUAL_DATA_VOLUME,

    @SerialName("unusual_connection_pattern")
    UNUSUAL_CONNECTION_PATTERN,

    @SerialName("new_remote_host")
    NEW_REMOTE_HOST,

    @SerialName("unusual_port")
    UNUSUAL_PORT,

    @SerialName("night_time_activity")
    NIGHT_TIME_ACTIVITY,

    @SerialName("background_spike")
    BACKGROUND_SPIKE,

    @SerialName("geographic_anomaly")
    GEOGRAPHIC_ANOMALY,
}

@Serializable
data class PrivacyConcern(
    val type: PrivacyConcernType,
    val severity: IssueSeverity,
    val description: String,
    val affectedApps: List<String>,
    val dataTypes: List<String>,
    val recommendation: String,
)

@Serializable
enum class PrivacyConcernType {
    @SerialName("location_tracking")
    LOCATION_TRACKING,

    @SerialName("device_fingerprinting")
    DEVICE_FINGERPRINTING,

    @SerialName("cross_app_tracking")
    CROSS_APP_TRACKING,

    @SerialName("sensitive_data_transmission")
    SENSITIVE_DATA_TRANSMISSION,

    @SerialName("excessive_permissions")
    EXCESSIVE_PERMISSIONS,

    @SerialName("third_party_sharing")
    THIRD_PARTY_SHARING,
}

@Serializable
data class NetworkRecommendation(
    val id: String,
    val priority: Int,
    val title: String,
    val description: String,
    val actionType: String,
    val affectedApps: List<String> = emptyList(),
)

package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ScanTargetType {
    @SerialName("url")
    URL,

    @SerialName("file")
    FILE,

    @SerialName("vpn_config")
    VPN_CONFIG,

    @SerialName("instagram")
    INSTAGRAM,
}

@Serializable
data class ScanRequest(
    val targetType: ScanTargetType,
    val rawInput: String,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class Provider {
    @SerialName("virus_total")
    VIRUS_TOTAL,

    @SerialName("google_safe_browsing")
    GOOGLE_SAFE_BROWSING,

    @SerialName("urlscan")
    URL_SCAN,

    @SerialName("urlhaus")
    URL_HAUS,

    @SerialName("phish_stats")
    PHISH_STATS,

    @SerialName("threat_fox")
    THREAT_FOX,

    @SerialName("network")
    NETWORK,

    @SerialName("file_static")
    FILE_STATIC,

    @SerialName("vpn_config")
    VPN_CONFIG,

    @SerialName("instagram")
    INSTAGRAM,

    @SerialName("ml")
    ML,

    @SerialName("local_heuristic")
    LOCAL_HEURISTIC,

    @SerialName("chat_gpt")
    CHAT_GPT,

    @SerialName("manual")
    MANUAL,
}

@Serializable
enum class VerdictStatus {
    @SerialName("clean")
    CLEAN,

    @SerialName("suspicious")
    SUSPICIOUS,

    @SerialName("malicious")
    MALICIOUS,

    @SerialName("unknown")
    UNKNOWN,

    @SerialName("error")
    ERROR,
}

@Serializable
data class VendorVerdict(
    val provider: Provider,
    val status: VerdictStatus,
    val score: Double,
    val details: Map<String, String?> = emptyMap(),
)

@Serializable
data class NetworkReport(
    val tlsVersion: String? = null,
    val cipherSuite: String? = null,
    val certValid: Boolean? = null,
    val headers: Map<String, String> = emptyMap(),
    val dnssecSignal: Boolean? = null,
)

@Serializable
data class FeatureWeight(
    val feature: String,
    val weight: Double,
)

@Serializable
data class MlReport(
    val probability: Double,
    val topFeatures: List<FeatureWeight> = emptyList(),
)

@Serializable
enum class IssueSeverity {
    @SerialName("low")
    LOW,

    @SerialName("medium")
    MEDIUM,

    @SerialName("high")
    HIGH,

    @SerialName("critical")
    CRITICAL,
}

@Serializable
data class ScanIssue(
    val id: String,
    val title: String,
    val severity: IssueSeverity,
    val description: String? = null,
)

@Serializable
data class FileScanReport(
    val fileName: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val sha256: String? = null,
    val suspiciousStrings: List<String> = emptyList(),
    val issues: List<ScanIssue> = emptyList(),
    val riskScore: Double = 0.0,
)

@Serializable
data class VpnConfigReport(
    val clientType: String? = null,
    val outboundType: String? = null,
    val serverAddress: String? = null,
    val port: Int? = null,
    val tlsEnabled: Boolean? = null,
    val insecureTransports: List<String> = emptyList(),
    val issues: List<ScanIssue> = emptyList(),
    val riskScore: Double = 0.0,
)

@Serializable
data class InstagramScanReport(
    val handle: String,
    val displayName: String? = null,
    val followerCount: Int? = null,
    val suspiciousSignals: List<String> = emptyList(),
    val issues: List<ScanIssue> = emptyList(),
    val riskScore: Double = 0.0,
)

@Serializable
enum class RiskCategory {
    @SerialName("minimal")
    MINIMAL,

    @SerialName("low")
    LOW,

    @SerialName("medium")
    MEDIUM,

    @SerialName("high")
    HIGH,

    @SerialName("critical")
    CRITICAL,
}

@Serializable
data class RiskBreakdown(
    val categories: Map<RiskCategory, Double> = emptyMap(),
)

@Serializable
data class ScanResult(
    val sessionId: String,
    val targetType: ScanTargetType,
    val targetLabel: String,
    val normalizedUrl: String? = null,
    val vendors: List<VendorVerdict> = emptyList(),
    val network: NetworkReport? = null,
    val ml: MlReport? = null,
    val file: FileScanReport? = null,
    val vpn: VpnConfigReport? = null,
    val instagram: InstagramScanReport? = null,
    val risk: Double,
    val breakdown: RiskBreakdown = RiskBreakdown(),
    @Serializable(with = InstantIso8601Serializer::class)
    val createdAt: Instant,
)

enum class ScanStage {
    INITIALIZING,
    NORMALIZING,
    FETCHING_THREAT_INTEL,
    ANALYZING_NETWORK_SECURITY,
    RUNNING_ML,
    ANALYZING_FILE,
    ANALYZING_VPN_CONFIG,
    ANALYZING_INSTAGRAM,
    AGGREGATING,
    COMPLETED,
    FAILED,
}

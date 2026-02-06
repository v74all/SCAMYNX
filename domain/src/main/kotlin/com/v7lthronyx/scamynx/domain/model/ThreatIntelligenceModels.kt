package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class ThreatType {
    @SerialName("malware")
    MALWARE,

    @SerialName("phishing")
    PHISHING,

    @SerialName("ransomware")
    RANSOMWARE,

    @SerialName("spyware")
    SPYWARE,

    @SerialName("adware")
    ADWARE,

    @SerialName("trojan")
    TROJAN,

    @SerialName("worm")
    WORM,

    @SerialName("rootkit")
    ROOTKIT,

    @SerialName("botnet")
    BOTNET,

    @SerialName("cryptojacker")
    CRYPTOJACKER,

    @SerialName("scam")
    SCAM,

    @SerialName("pup")
    PUP,

    @SerialName("exploit")
    EXPLOIT,

    @SerialName("c2")
    C2,

    @SerialName("data_theft")
    DATA_THEFT,

    @SerialName("credential_harvesting")
    CREDENTIAL_HARVESTING,

    @SerialName("social_engineering")
    SOCIAL_ENGINEERING,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
enum class AttackVector {
    @SerialName("web")
    WEB,

    @SerialName("email")
    EMAIL,

    @SerialName("sms")
    SMS,

    @SerialName("social_media")
    SOCIAL_MEDIA,

    @SerialName("messaging_app")
    MESSAGING_APP,

    @SerialName("drive_by")
    DRIVE_BY,

    @SerialName("malvertising")
    MALVERTISING,

    @SerialName("supply_chain")
    SUPPLY_CHAIN,

    @SerialName("physical")
    PHYSICAL,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
enum class ThreatActorLevel {
    @SerialName("script_kiddie")
    SCRIPT_KIDDIE,

    @SerialName("cybercriminal")
    CYBERCRIMINAL,

    @SerialName("organized_crime")
    ORGANIZED_CRIME,

    @SerialName("apt")
    APT,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
enum class ConfidenceLevel {
    @SerialName("very_low")
    VERY_LOW,

    @SerialName("low")
    LOW,

    @SerialName("medium")
    MEDIUM,

    @SerialName("high")
    HIGH,

    @SerialName("very_high")
    VERY_HIGH,
}


@Serializable
enum class IoCType {
    @SerialName("domain")
    DOMAIN,

    @SerialName("url")
    URL,

    @SerialName("ip_address")
    IP_ADDRESS,

    @SerialName("file_hash_md5")
    FILE_HASH_MD5,

    @SerialName("file_hash_sha1")
    FILE_HASH_SHA1,

    @SerialName("file_hash_sha256")
    FILE_HASH_SHA256,

    @SerialName("email_address")
    EMAIL_ADDRESS,

    @SerialName("phone_number")
    PHONE_NUMBER,

    @SerialName("bitcoin_address")
    BITCOIN_ADDRESS,

    @SerialName("crypto_wallet")
    CRYPTO_WALLET,

    @SerialName("registry_key")
    REGISTRY_KEY,

    @SerialName("mutex")
    MUTEX,

    @SerialName("user_agent")
    USER_AGENT,

    @SerialName("ssl_certificate")
    SSL_CERTIFICATE,

    @SerialName("asn")
    ASN,
}

@Serializable
data class IndicatorOfCompromise(
    val id: String,
    val type: IoCType,
    val value: String,
    val threatTypes: List<ThreatType>,
    val confidence: ConfidenceLevel,
    val severity: IssueSeverity,
    val firstSeen: Instant? = null,
    val lastSeen: Instant? = null,
    val tags: List<String> = emptyList(),
    val relatedIoCs: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val description: String? = null,
)


@Serializable
enum class MitreTactic {
    @SerialName("reconnaissance")
    RECONNAISSANCE,

    @SerialName("resource_development")
    RESOURCE_DEVELOPMENT,

    @SerialName("initial_access")
    INITIAL_ACCESS,

    @SerialName("execution")
    EXECUTION,

    @SerialName("persistence")
    PERSISTENCE,

    @SerialName("privilege_escalation")
    PRIVILEGE_ESCALATION,

    @SerialName("defense_evasion")
    DEFENSE_EVASION,

    @SerialName("credential_access")
    CREDENTIAL_ACCESS,

    @SerialName("discovery")
    DISCOVERY,

    @SerialName("lateral_movement")
    LATERAL_MOVEMENT,

    @SerialName("collection")
    COLLECTION,

    @SerialName("command_and_control")
    COMMAND_AND_CONTROL,

    @SerialName("exfiltration")
    EXFILTRATION,

    @SerialName("impact")
    IMPACT,
}

@Serializable
data class MitreTechnique(
    val id: String,
    val name: String,
    val tactic: MitreTactic,
    val url: String? = null,
)

@Serializable
data class GeoAttribution(
    val countryCode: String? = null,
    val countryName: String? = null,
    val region: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val asn: String? = null,
    val asnOrg: String? = null,
    val isp: String? = null,
    val isVpn: Boolean = false,
    val isTor: Boolean = false,
    val isProxy: Boolean = false,
    val isHosting: Boolean = false,
)

@Serializable
data class ThreatActorProfile(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val level: ThreatActorLevel,
    val motivation: String? = null,
    val targetedSectors: List<String> = emptyList(),
    val targetedCountries: List<String> = emptyList(),
    val associatedMalware: List<String> = emptyList(),
    val ttps: List<MitreTechnique> = emptyList(),
    val firstObserved: Instant? = null,
    val lastObserved: Instant? = null,
    val description: String? = null,
)

@Serializable
data class ThreatTimelineEvent(
    val timestamp: Instant,
    val eventType: String,
    val description: String,
    val source: String? = null,
    val severity: IssueSeverity = IssueSeverity.LOW,
)

@Serializable
data class MitigationAction(
    val id: String,
    val priority: Int,
    val title: String,
    val description: String,
    val actionType: MitigationActionType,
    val automated: Boolean = false,
    val estimatedTime: String? = null,
)

@Serializable
enum class MitigationActionType {
    @SerialName("block")
    BLOCK,

    @SerialName("quarantine")
    QUARANTINE,

    @SerialName("delete")
    DELETE,

    @SerialName("update")
    UPDATE,

    @SerialName("configure")
    CONFIGURE,

    @SerialName("monitor")
    MONITOR,

    @SerialName("report")
    REPORT,

    @SerialName("educate")
    EDUCATE,

    @SerialName("investigate")
    INVESTIGATE,
}

@Serializable
data class ThreatIntelligenceReport(
    val id: String,
    val target: String,
    val targetType: IoCType,

    val threatTypes: List<ThreatType>,
    val primaryThreatType: ThreatType,
    val attackVectors: List<AttackVector>,
    val severity: IssueSeverity,
    val confidence: ConfidenceLevel,
    val riskScore: Double,

    val geoAttribution: GeoAttribution? = null,
    val threatActor: ThreatActorProfile? = null,

    val indicators: List<IndicatorOfCompromise> = emptyList(),
    val ttps: List<MitreTechnique> = emptyList(),
    val relatedThreats: List<String> = emptyList(),

    val firstSeen: Instant? = null,
    val lastSeen: Instant? = null,
    val timeline: List<ThreatTimelineEvent> = emptyList(),

    val mitigations: List<MitigationAction> = emptyList(),
    val isBlocked: Boolean = false,
    val isQuarantined: Boolean = false,

    val sources: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val summary: String? = null,
    @Serializable(with = InstantIso8601Serializer::class)
    val generatedAt: Instant,
    val expiresAt: Instant? = null,
)


@Serializable
enum class ThreatFeedStatus {
    @SerialName("active")
    ACTIVE,

    @SerialName("paused")
    PAUSED,

    @SerialName("error")
    ERROR,

    @SerialName("expired")
    EXPIRED,
}

@Serializable
data class ThreatFeedSource(
    val id: String,
    val name: String,
    val provider: String,
    val feedType: String,
    val status: ThreatFeedStatus,
    val lastSync: Instant? = null,
    val nextSync: Instant? = null,
    val iocCount: Int = 0,
    val isPremium: Boolean = false,
    val reliability: ConfidenceLevel = ConfidenceLevel.MEDIUM,
)

@Serializable
data class ThreatLandscapeSummary(
    val totalThreatsDetected: Int,
    val criticalThreats: Int,
    val highThreats: Int,
    val mediumThreats: Int,
    val lowThreats: Int,
    val topThreatTypes: Map<ThreatType, Int>,
    val topAttackVectors: Map<AttackVector, Int>,
    val activeFeeds: Int,
    val lastUpdated: Instant,
    val trendingThreats: List<String> = emptyList(),
    val emergingThreats: List<String> = emptyList(),
)

package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class DarkWebMonitoringConfig(
    val isEnabled: Boolean,
    val monitoredAssets: List<MonitoredAsset>,
    val alertPreferences: AlertPreferences,
    val scanFrequency: ScanFrequency,
    val lastScan: Instant? = null,
    val nextScan: Instant? = null,
    val isPremium: Boolean = false,
)

@Serializable
data class MonitoredAsset(
    val id: String,
    val type: AssetType,
    val value: String,
    val maskedValue: String,
    val isVerified: Boolean,
    val addedAt: Instant,
    val lastChecked: Instant? = null,
    val exposureCount: Int = 0,
    val status: MonitoringStatus,
)

@Serializable
enum class AssetType {
    @SerialName("email")
    EMAIL,

    @SerialName("phone")
    PHONE,

    @SerialName("username")
    USERNAME,

    @SerialName("domain")
    DOMAIN,

    @SerialName("credit_card")
    CREDIT_CARD,

    @SerialName("ssn")
    SSN,

    @SerialName("passport")
    PASSPORT,

    @SerialName("driver_license")
    DRIVER_LICENSE,

    @SerialName("bank_account")
    BANK_ACCOUNT,

    @SerialName("crypto_wallet")
    CRYPTO_WALLET,
}

@Serializable
enum class MonitoringStatus {
    @SerialName("active")
    ACTIVE,

    @SerialName("paused")
    PAUSED,

    @SerialName("pending_verification")
    PENDING_VERIFICATION,

    @SerialName("expired")
    EXPIRED,
}

@Serializable
data class AlertPreferences(
    val emailAlerts: Boolean = true,
    val pushNotifications: Boolean = true,
    val smsAlerts: Boolean = false,
    val alertOnNewExposure: Boolean = true,
    val alertOnHighSeverity: Boolean = true,
    val weeklyDigest: Boolean = true,
    val monthlyReport: Boolean = true,
)

@Serializable
enum class ScanFrequency {
    @SerialName("realtime")
    REALTIME,

    @SerialName("daily")
    DAILY,

    @SerialName("weekly")
    WEEKLY,

    @SerialName("monthly")
    MONTHLY,
}


@Serializable
data class DarkWebExposure(
    val id: String,
    val assetId: String,
    val assetType: AssetType,
    val maskedAsset: String,

    val source: ExposureSource,
    val marketplace: String? = null,
    val forumName: String? = null,
    val pastebin: String? = null,

    val exposureType: ExposureType,
    val exposedData: List<ExposedDataType>,
    val severity: BreachSeverity,
    val confidence: ConfidenceLevel,

    val context: ExposureContext? = null,
    val relatedExposures: List<String> = emptyList(),

    val discoveredAt: Instant,
    val estimatedExposureDate: Instant? = null,
    val lastSeenAt: Instant? = null,
    val isActive: Boolean = true,

    val isAcknowledged: Boolean = false,
    val acknowledgedAt: Instant? = null,
    val remediationStatus: RemediationStatus = RemediationStatus.PENDING,
    val recommendations: List<String> = emptyList(),
)

@Serializable
enum class ExposureSource {
    @SerialName("dark_web_marketplace")
    DARK_WEB_MARKETPLACE,

    @SerialName("hacker_forum")
    HACKER_FORUM,

    @SerialName("paste_site")
    PASTE_SITE,

    @SerialName("telegram_channel")
    TELEGRAM_CHANNEL,

    @SerialName("data_dump")
    DATA_DUMP,

    @SerialName("ransomware_leak")
    RANSOMWARE_LEAK,

    @SerialName("stealer_log")
    STEALER_LOG,

    @SerialName("combo_list")
    COMBO_LIST,

    @SerialName("social_media")
    SOCIAL_MEDIA,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
enum class ExposureType {
    @SerialName("credential_leak")
    CREDENTIAL_LEAK,

    @SerialName("pii_leak")
    PII_LEAK,

    @SerialName("financial_data")
    FINANCIAL_DATA,

    @SerialName("for_sale")
    FOR_SALE,

    @SerialName("freely_available")
    FREELY_AVAILABLE,

    @SerialName("targeted_attack")
    TARGETED_ATTACK,

    @SerialName("botnet_log")
    BOTNET_LOG,

    @SerialName("phishing_kit")
    PHISHING_KIT,
}

@Serializable
data class ExposureContext(
    val breachName: String? = null,
    val breachDate: String? = null,
    val affectedRecords: Long? = null,
    val affectedCompany: String? = null,
    val priceIfForSale: String? = null,
    val currency: String? = null,
    val sellerReputation: String? = null,
    val additionalInfo: Map<String, String> = emptyMap(),
)

@Serializable
enum class RemediationStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("in_progress")
    IN_PROGRESS,

    @SerialName("completed")
    COMPLETED,

    @SerialName("not_applicable")
    NOT_APPLICABLE,
}


@Serializable
data class DarkWebMonitoringReport(
    
    val totalExposures: Int,
    val newExposures: Int,
    val criticalExposures: Int,
    val activeExposures: Int,
    val resolvedExposures: Int,

    val overallRiskScore: Double,
    val riskLevel: BreachSeverity,
    val riskTrend: RiskTrend,

    val exposuresByAsset: Map<String, Int>,
    val exposuresByType: Map<ExposureType, Int>,
    val exposuresBySource: Map<ExposureSource, Int>,
    val exposuresBySeverity: Map<BreachSeverity, Int>,

    val exposures: List<DarkWebExposure>,
    val recentActivity: List<DarkWebActivity>,

    val prioritizedActions: List<RemediationAction>,
    val securityTips: List<String>,

    val monitoredAssets: Int,
    val lastFullScan: Instant,
    val reportGeneratedAt: Instant,
    val reportPeriod: ReportPeriod,
)

@Serializable
data class ReportPeriod(
    val start: Instant,
    val end: Instant,
)

@Serializable
data class DarkWebActivity(
    val id: String,
    val activityType: ActivityType,
    val description: String,
    val severity: BreachSeverity,
    val timestamp: Instant,
    val relatedExposureId: String? = null,
)

@Serializable
enum class ActivityType {
    @SerialName("new_exposure")
    NEW_EXPOSURE,

    @SerialName("exposure_updated")
    EXPOSURE_UPDATED,

    @SerialName("exposure_resolved")
    EXPOSURE_RESOLVED,

    @SerialName("price_change")
    PRICE_CHANGE,

    @SerialName("listing_removed")
    LISTING_REMOVED,

    @SerialName("new_mention")
    NEW_MENTION,

    @SerialName("credential_verified")
    CREDENTIAL_VERIFIED,
}

@Serializable
data class RemediationAction(
    val id: String,
    val priority: Int,
    val title: String,
    val description: String,
    val actionType: RemediationActionType,
    val affectedAssets: List<String>,
    val estimatedTime: String? = null,
    val automatable: Boolean = false,
    val externalLink: String? = null,
)

@Serializable
enum class RemediationActionType {
    @SerialName("change_password")
    CHANGE_PASSWORD,

    @SerialName("enable_2fa")
    ENABLE_2FA,

    @SerialName("freeze_credit")
    FREEZE_CREDIT,

    @SerialName("contact_bank")
    CONTACT_BANK,

    @SerialName("report_fraud")
    REPORT_FRAUD,

    @SerialName("monitor_accounts")
    MONITOR_ACCOUNTS,

    @SerialName("update_security_questions")
    UPDATE_SECURITY_QUESTIONS,

    @SerialName("review_permissions")
    REVIEW_PERMISSIONS,

    @SerialName("scan_devices")
    SCAN_DEVICES,

    @SerialName("notify_contacts")
    NOTIFY_CONTACTS,
}


@Serializable
data class CredentialIntelligence(
    val email: String,
    val maskedEmail: String,

    val totalExposures: Int,
    val uniquePasswords: Int,
    val plaintextPasswords: Int,
    val hashedPasswords: Int,

    val passwordPatterns: List<PasswordPattern>,
    val commonPasswords: List<String>,
    val passwordStrengthDistribution: Map<PasswordStrength, Int>,

    val associatedUsernames: List<String>,
    val associatedPhones: List<String>,
    val associatedNames: List<String>,
    val associatedAddresses: List<String>,

    val riskFactors: List<CredentialRiskFactor>,
    val recommendations: List<String>,

    val analyzedAt: Instant,
)

@Serializable
data class PasswordPattern(
    val pattern: String,
    val occurrences: Int,
    val examples: List<String>,
    val riskLevel: BreachSeverity,
)

@Serializable
data class CredentialRiskFactor(
    val factor: String,
    val severity: BreachSeverity,
    val description: String,
    val affectedBreaches: Int,
)


@Serializable
data class IdentityProtectionStatus(
    val isProtected: Boolean,
    val protectionLevel: IdentityProtectionLevel,
    val coveredAssets: List<AssetType>,
    val activeAlerts: Int,
    val resolvedAlerts: Int,
    val lastIncident: Instant? = null,
    val insuranceCoverage: InsuranceCoverage? = null,
    val restorationServices: Boolean = false,
)

@Serializable
enum class IdentityProtectionLevel {
    @SerialName("basic")
    BASIC,

    @SerialName("standard")
    STANDARD,

    @SerialName("premium")
    PREMIUM,

    @SerialName("enterprise")
    ENTERPRISE,
}

@Serializable
data class InsuranceCoverage(
    val isActive: Boolean,
    val coverageAmount: String,
    val currency: String,
    val expiresAt: Instant,
    val coveredIncidents: List<String>,
)


@Serializable
data class DarkWebAlert(
    val id: String,
    val type: AlertType,
    val severity: BreachSeverity,
    val title: String,
    val message: String,
    val exposureId: String? = null,
    val assetId: String? = null,
    val isRead: Boolean = false,
    val isActioned: Boolean = false,
    val createdAt: Instant,
    val expiresAt: Instant? = null,
    val actions: List<AlertAction>,
)

@Serializable
enum class AlertType {
    @SerialName("new_exposure")
    NEW_EXPOSURE,

    @SerialName("critical_exposure")
    CRITICAL_EXPOSURE,

    @SerialName("credential_for_sale")
    CREDENTIAL_FOR_SALE,

    @SerialName("identity_theft_risk")
    IDENTITY_THEFT_RISK,

    @SerialName("financial_risk")
    FINANCIAL_RISK,

    @SerialName("weekly_summary")
    WEEKLY_SUMMARY,

    @SerialName("remediation_reminder")
    REMEDIATION_REMINDER,
}

@Serializable
data class AlertAction(
    val id: String,
    val label: String,
    val actionType: String,
    val isPrimary: Boolean = false,
    val url: String? = null,
)

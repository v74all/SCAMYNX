package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class BreachExposure(
    val breachId: String,
    val breachName: String,
    val breachDate: String? = null,
    val exposedData: List<ExposedDataType>,
    val severity: BreachSeverity,
    val description: String? = null,
    val verified: Boolean = false,
    @Serializable(with = InstantIso8601Serializer::class)
    val detectedAt: Instant,
)

@Serializable
enum class ExposedDataType {
    EMAIL,
    PHONE_NUMBER,
    PASSWORD,
    USERNAME,
    IP_ADDRESS,
    CREDIT_CARD,
    SOCIAL_SECURITY_NUMBER,
    FULL_NAME,
    ADDRESS,
    DATE_OF_BIRTH,
}

@Serializable
enum class BreachSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

@Serializable
data class BreachCheckResult(
    val identifier: String,
    val identifierType: BreachIdentifierType,
    val isExposed: Boolean,
    val exposures: List<BreachExposure> = emptyList(),
    val totalBreachCount: Int,
    val riskScore: Double,
    val recommendations: List<String> = emptyList(),
    @Serializable(with = InstantIso8601Serializer::class)
    val checkedAt: Instant,
)

@Serializable
enum class BreachIdentifierType {
    EMAIL,
    PHONE_NUMBER,
    USERNAME,
}

@Serializable
data class BreachMonitoringReport(
    val emailChecks: List<BreachCheckResult> = emptyList(),
    val phoneChecks: List<BreachCheckResult> = emptyList(),
    val usernameChecks: List<BreachCheckResult> = emptyList(),
    val overallRiskScore: Double,
    val totalExposures: Int,
    val criticalExposures: Int,
    val monitoringEnabled: Boolean,
    val lastSyncAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class)
    val reportGeneratedAt: Instant,
)

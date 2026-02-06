package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SecurityScoreComponent(
    val category: SecurityCategory,
    val score: Int,
    val maxScore: Int = 100,
    val weight: Double,
    val status: SecurityStatus,
    val issues: List<SecurityIssue> = emptyList(),
    val recommendations: List<String> = emptyList(),
)

@Serializable
enum class SecurityCategory {
    PRIVACY_RADAR,
    PASSWORD_SECURITY,
    WIFI_SECURITY,
    APP_PERMISSIONS,
    DEVICE_HARDENING,
    BREACH_EXPOSURE,
    ANTI_PHISHING,
    SOCIAL_ENGINEERING,
}

@Serializable
enum class SecurityStatus {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL,
}

@Serializable
data class SecurityIssue(
    val severity: IssueSeverity,
    val title: String,
    val description: String,
    val actionable: Boolean = true,
    val actionLabel: String? = null,
)

@Serializable
data class SecurityScoreReport(
    val overallScore: Int,
    val status: SecurityStatus,
    val components: List<SecurityScoreComponent>,
    val topRecommendations: List<String>,
    val shareableBadge: SecurityBadge,
    @Serializable(with = InstantIso8601Serializer::class)
    val timestamp: Instant,
)

@Serializable
data class SecurityBadge(
    val score: Int,
    val status: SecurityStatus,
    val badgeText: String,
    val badgeColor: String,
    val shareableText: String,
)

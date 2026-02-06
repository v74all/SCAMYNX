package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant

enum class ScamIndicator {
    URGENCY,
    PAYMENT_REQUEST,
    ACCOUNT_TAKEOVER,
    GOVERNMENT_THREAT,
    GIVEAWAY,
    IMPERSONATION,
    LINK_OBFUSCATION,
}

enum class SocialEngineeringRisk {
    LOW,
    MEDIUM,
    HIGH,
}

data class SocialEngineeringReport(
    val originalMessage: String,
    val normalizedMessage: String,
    val riskScore: Double,
    val riskLevel: SocialEngineeringRisk,
    val indicators: List<ScamIndicator>,
    val highlightSnippets: List<String>,
    val recommendations: List<String>,
    val timestamp: Instant,
)

package com.v7lthronyx.scamynx.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThreatFeedResponseDto(
    val indicators: List<ThreatIndicatorDto> = emptyList(),
    @SerialName("generated_at")
    val generatedAt: String? = null,
)

@Serializable
data class ThreatIndicatorDto(
    val id: String,
    val url: String,
    @SerialName("risk_score")
    val riskScore: Double,
    val tags: List<String> = emptyList(),
    @SerialName("last_seen")
    val lastSeen: String? = null,
    val source: String,
)

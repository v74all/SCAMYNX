package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class LinkDisposition {
    SAFE,
    SUSPICIOUS,
    MALICIOUS,
}

@Serializable
data class AntiPhishingAnalysis(
    val url: String,
    val normalizedUrl: String,
    val score: Double,
    val disposition: LinkDisposition,
    val triggers: List<String> = emptyList(),
    val reputationMatches: List<String> = emptyList(),
    val inspectedAt: Instant,
)

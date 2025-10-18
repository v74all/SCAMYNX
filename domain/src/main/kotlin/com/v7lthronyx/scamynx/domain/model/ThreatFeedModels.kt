package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant

data class ThreatIndicator(
    val id: String,
    val url: String,
    val riskScore: Double,
    val tags: List<String> = emptyList(),
    val lastSeen: Instant?,
    val source: String,
)

data class ThreatFeedSyncResult(
    val fetchedCount: Int,
    val skipped: Boolean,
    val fetchedAt: Instant,
)

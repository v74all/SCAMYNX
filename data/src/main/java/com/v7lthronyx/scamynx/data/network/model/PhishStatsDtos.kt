package com.v7lthronyx.scamynx.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhishStatsRecordDto(
    val id: Long? = null,
    val url: String? = null,
    val host: String? = null,
    val target: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("first_seen")
    val firstSeen: String? = null,
    @SerialName("last_seen")
    val lastSeen: String? = null,
)

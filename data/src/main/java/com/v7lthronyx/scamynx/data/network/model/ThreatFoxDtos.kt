package com.v7lthronyx.scamynx.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThreatFoxSearchRequestDto(
    val query: String,
    @SerialName("search_term")
    val searchTerm: String,
)

@Serializable
data class ThreatFoxSearchResponseDto(
    @SerialName("query_status")
    val queryStatus: String,
    val data: List<ThreatFoxIndicatorDto>? = null,
    @SerialName("error_message")
    val errorMessage: String? = null,
)

@Serializable
data class ThreatFoxIndicatorDto(
    val ioc: String? = null,
    @SerialName("threat_type")
    val threatType: String? = null,
    @SerialName("confidence_level")
    val confidenceLevel: Int? = null,
    val malware: String? = null,
    val reference: String? = null,
    val tags: List<String>? = null,
)

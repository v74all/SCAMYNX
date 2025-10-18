package com.v7lthronyx.scamynx.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UrlHausLookupRequestDto(
    val url: String,
)

@Serializable
data class UrlHausLookupResponseDto(
    @SerialName("query_status")
    val queryStatus: String,
    val url: String? = null,
    @SerialName("url_status")
    val urlStatus: String? = null,
    val threat: String? = null,
    @SerialName("threat_tags")
    val threatTags: List<String>? = null,
    val host: String? = null,
    @SerialName("last_online")
    val lastOnline: String? = null,
    val blacklists: UrlHausBlacklistsDto? = null,
)

@Serializable
data class UrlHausBlacklistsDto(
    val urlhaus: UrlHausBlacklistEntryDto? = null,
    val surbl: UrlHausBlacklistEntryDto? = null,
    @SerialName("phishtank")
    val phishTank: UrlHausBlacklistEntryDto? = null,
    @SerialName("google_safebrowsing")
    val googleSafeBrowsing: UrlHausBlacklistEntryDto? = null,
)

@Serializable
data class UrlHausBlacklistEntryDto(
    val status: String? = null,
)

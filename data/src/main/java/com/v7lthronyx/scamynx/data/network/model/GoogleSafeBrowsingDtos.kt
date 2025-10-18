package com.v7lthronyx.scamynx.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleSafeBrowsingRequestDto(
    @SerialName("client")
    val client: ClientDto = ClientDto(),
    @SerialName("threatInfo")
    val threatInfo: ThreatInfoDto,
) {
    @Serializable
    data class ClientDto(
        @SerialName("clientId") val clientId: String = "scamynx",
        @SerialName("clientVersion") val clientVersion: String = "0.1",
    )

    @Serializable
    data class ThreatInfoDto(
        @SerialName("threatTypes") val threatTypes: List<String>,
        @SerialName("platformTypes") val platformTypes: List<String>,
        @SerialName("threatEntryTypes") val threatEntryTypes: List<String>,
        @SerialName("threatEntries") val threatEntries: List<ThreatEntryDto>,
    ) {
        @Serializable
        data class ThreatEntryDto(
            val url: String,
        )
    }
}

@Serializable
data class GoogleSafeBrowsingResponseDto(
    @SerialName("matches")
    val matches: List<ThreatMatchDto>? = null,
) {
    @Serializable
    data class ThreatMatchDto(
        @SerialName("threatType") val threatType: String? = null,
        @SerialName("platformType") val platformType: String? = null,
        @SerialName("threatEntryType") val threatEntryType: String? = null,
        @SerialName("threat") val threat: ThreatDto? = null,
        @SerialName("threatEntryMetadata") val threatEntryMetadata: ThreatEntryMetadataDto? = null,
        @SerialName("cacheDuration") val cacheDuration: String? = null,
    ) {
        @Serializable
        data class ThreatDto(
            val url: String? = null,
        )
        
        @Serializable
        data class ThreatEntryMetadataDto(
            @SerialName("entries") val entries: List<MetadataEntryDto>? = null,
        ) {
            @Serializable
            data class MetadataEntryDto(
                @SerialName("key") val key: String? = null,
                @SerialName("value") val value: String? = null,
            )
        }
    }
}

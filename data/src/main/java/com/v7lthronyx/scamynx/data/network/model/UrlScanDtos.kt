package com.v7lthronyx.scamynx.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UrlScanSubmitRequestDto(
    val url: String,
    @SerialName("public") val isPublic: String = "on",
)

@Serializable
data class UrlScanSubmitResponseDto(
    val message: String? = null,
    val uuid: String? = null,
    val result: String? = null,
    val api: String? = null,
    val visibility: String? = null,
    val options: OptionsDto? = null,
    val url: String? = null,
    val country: String? = null,
) {
    @Serializable
    data class OptionsDto(
        val useragent: String? = null,
    )
}

@Serializable
data class UrlScanErrorDto(
    val message: String? = null,
    val description: String? = null,
    val status: Int? = null,
)

@Serializable
data class UrlScanResultDto(
    val task: TaskDto? = null,
    val stats: StatsDto? = null,
    val page: PageDto? = null,
    val verdicts: VerdictsDto? = null,
    val meta: MetaDto? = null,
) {
    @Serializable
    data class TaskDto(
        val uuid: String? = null,
        val time: String? = null,
        val url: String? = null,
        val visibility: String? = null,
        val method: String? = null,
    )

    @Serializable
    data class StatsDto(
        val malicious: Int? = null,
        val suspicious: Int? = null,
        val undetected: Int? = null,
        val harmless: Int? = null,
    )

    @Serializable
    data class PageDto(
        val url: String? = null,
        val domain: String? = null,
        val country: String? = null,
        val city: String? = null,
        val server: String? = null,
        val ip: String? = null,
        val ptr: String? = null,
    )
    
    @Serializable
    data class VerdictsDto(
        val overall: OverallVerdictDto? = null,
        val urlscan: UrlscanVerdictDto? = null,
        val engines: EnginesVerdictDto? = null,
        val community: CommunityVerdictDto? = null,
    ) {
        @Serializable
        data class OverallVerdictDto(
            val malicious: Boolean? = null,
            val suspicious: Boolean? = null,
            val hasVerdicts: Int? = null,
        )
        
        @Serializable
        data class UrlscanVerdictDto(
            val malicious: Boolean? = null,
            val suspicious: Boolean? = null,
            val score: Int? = null,
        )
        
        @Serializable
        data class EnginesVerdictDto(
            val malicious: List<String>? = null,
            val suspicious: List<String>? = null,
            val benign: List<String>? = null,
        )
        
        @Serializable
        data class CommunityVerdictDto(
            val votesMalicious: Int? = null,
            val votesSuspicious: Int? = null,
            val votesBenign: Int? = null,
        )
    }
    
    @Serializable
    data class MetaDto(
        val processors: ProcessorsDto? = null,
    ) {
        @Serializable
        data class ProcessorsDto(
            val abp: AbpDto? = null,
            val gsb: GsbDto? = null,
        ) {
            @Serializable
            data class AbpDto(
                val state: String? = null,
            )
            
            @Serializable
            data class GsbDto(
                val state: String? = null,
            )
        }
    }
}

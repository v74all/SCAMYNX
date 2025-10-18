package com.v7lthronyx.scamynx.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VirusTotalSubmitRequestDto(
    val url: String,
)

@Serializable
data class VirusTotalSubmitResponseDto(
    val data: VirusTotalSubmitDataDto? = null,
    val error: VirusTotalErrorDto? = null,
) {
    @Serializable
    data class VirusTotalSubmitDataDto(
        val id: String? = null,
        val type: String? = null,
        val attributes: SubmitAttributesDto? = null,
    ) {
        @Serializable
        data class SubmitAttributesDto(
            val date: Long? = null,
            val status: String? = null,
        )
    }
}

@Serializable
data class VirusTotalReportDto(
    val data: VirusTotalReportDataDto? = null,
    val error: ErrorDto? = null,
) {
    @Serializable
    data class VirusTotalReportDataDto(
        val id: String? = null,
        val type: String? = null,
        val attributes: AttributesDto? = null,
    ) {
        @Serializable
        data class AttributesDto(
            @SerialName("last_analysis_stats")
            val lastAnalysisStats: Map<String, Int>? = null,
            @SerialName("last_analysis_results")
            val lastAnalysisResults: Map<String, EngineResultDto>? = null,
            @SerialName("last_analysis_date")
            val lastAnalysisDate: Long? = null,
            @SerialName("last_submission_date")
            val lastSubmissionDate: Long? = null,
            @SerialName("reputation")
            val reputation: Int? = null,
            @SerialName("times_submitted")
            val timesSubmitted: Int? = null,
            @SerialName("total_votes")
            val totalVotes: TotalVotesDto? = null,
        ) {
            @Serializable
            data class EngineResultDto(
                val category: String? = null,
                val result: String? = null,
                val method: String? = null,
                @SerialName("engine_name")
                val engineName: String? = null,
                @SerialName("engine_version")
                val engineVersion: String? = null,
                @SerialName("engine_update")
                val engineUpdate: String? = null,
            )
            
            @Serializable
            data class TotalVotesDto(
                val harmless: Int? = null,
                val malicious: Int? = null,
            )
        }
    }
    
    @Serializable
    data class ErrorDto(
        val code: String? = null,
        val message: String? = null,
    )
}

@Serializable
data class VirusTotalErrorDto(
    val code: String? = null,
    val message: String? = null,
)

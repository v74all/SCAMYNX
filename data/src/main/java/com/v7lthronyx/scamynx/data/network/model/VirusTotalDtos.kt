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

/**
 * VirusTotal File Report DTO
 * https://docs.virustotal.com/reference/file-info
 */
@Serializable
data class VirusTotalFileReportDto(
    val data: FileReportDataDto? = null,
    val error: VirusTotalErrorDto? = null,
) {
    @Serializable
    data class FileReportDataDto(
        val id: String? = null,
        val type: String? = null,
        val attributes: FileAttributesDto? = null,
    )

    @Serializable
    data class FileAttributesDto(
        @SerialName("last_analysis_stats")
        val lastAnalysisStats: AnalysisStatsDto? = null,
        @SerialName("last_analysis_results")
        val lastAnalysisResults: Map<String, EngineResultDto>? = null,
        @SerialName("last_analysis_date")
        val lastAnalysisDate: Long? = null,
        @SerialName("first_submission_date")
        val firstSubmissionDate: Long? = null,
        @SerialName("last_submission_date")
        val lastSubmissionDate: Long? = null,
        @SerialName("times_submitted")
        val timesSubmitted: Int? = null,
        @SerialName("reputation")
        val reputation: Int? = null,
        @SerialName("type_description")
        val typeDescription: String? = null,
        @SerialName("type_tag")
        val typeTag: String? = null,
        @SerialName("meaningful_name")
        val meaningfulName: String? = null,
        val names: List<String>? = null,
        val size: Long? = null,
        val md5: String? = null,
        val sha1: String? = null,
        val sha256: String? = null,
        @SerialName("popular_threat_classification")
        val popularThreatClassification: ThreatClassificationDto? = null,
    )

    @Serializable
    data class AnalysisStatsDto(
        val harmless: Int? = null,
        @SerialName("type-unsupported")
        val typeUnsupported: Int? = null,
        val suspicious: Int? = null,
        @SerialName("confirmed-timeout")
        val confirmedTimeout: Int? = null,
        val timeout: Int? = null,
        val failure: Int? = null,
        val malicious: Int? = null,
        val undetected: Int? = null,
    )

    @Serializable
    data class EngineResultDto(
        val category: String? = null,
        @SerialName("engine_name")
        val engineName: String? = null,
        @SerialName("engine_version")
        val engineVersion: String? = null,
        @SerialName("engine_update")
        val engineUpdate: String? = null,
        val result: String? = null,
        val method: String? = null,
    )

    @Serializable
    data class ThreatClassificationDto(
        @SerialName("suggested_threat_label")
        val suggestedThreatLabel: String? = null,
        @SerialName("popular_threat_category")
        val popularThreatCategory: List<ThreatCategoryDto>? = null,
        @SerialName("popular_threat_name")
        val popularThreatName: List<ThreatNameDto>? = null,
    )

    @Serializable
    data class ThreatCategoryDto(
        val count: Int? = null,
        val value: String? = null,
    )

    @Serializable
    data class ThreatNameDto(
        val count: Int? = null,
        val value: String? = null,
    )
}

/**
 * VirusTotal IP Address Report DTO
 * https://docs.virustotal.com/reference/ip-info
 */
@Serializable
data class VirusTotalIpReportDto(
    val data: IpReportDataDto? = null,
    val error: VirusTotalErrorDto? = null,
) {
    @Serializable
    data class IpReportDataDto(
        val id: String? = null,
        val type: String? = null,
        val attributes: IpAttributesDto? = null,
    )

    @Serializable
    data class IpAttributesDto(
        @SerialName("last_analysis_stats")
        val lastAnalysisStats: IpAnalysisStatsDto? = null,
        @SerialName("last_analysis_results")
        val lastAnalysisResults: Map<String, IpEngineResultDto>? = null,
        @SerialName("last_analysis_date")
        val lastAnalysisDate: Long? = null,
        @SerialName("last_modification_date")
        val lastModificationDate: Long? = null,
        @SerialName("reputation")
        val reputation: Int? = null,
        val asn: Int? = null,
        @SerialName("as_owner")
        val asOwner: String? = null,
        val country: String? = null,
        val continent: String? = null,
        val network: String? = null,
        @SerialName("regional_internet_registry")
        val regionalInternetRegistry: String? = null,
        val whois: String? = null,
        @SerialName("whois_date")
        val whoisDate: Long? = null,
        val tags: List<String>? = null,
        @SerialName("total_votes")
        val totalVotes: TotalVotesDto? = null,
    )

    @Serializable
    data class IpAnalysisStatsDto(
        val harmless: Int? = null,
        val malicious: Int? = null,
        val suspicious: Int? = null,
        val undetected: Int? = null,
        val timeout: Int? = null,
    )

    @Serializable
    data class IpEngineResultDto(
        val category: String? = null,
        val result: String? = null,
        val method: String? = null,
        @SerialName("engine_name")
        val engineName: String? = null,
    )

    @Serializable
    data class TotalVotesDto(
        val harmless: Int? = null,
        val malicious: Int? = null,
    )
}

package com.v7lthronyx.scamynx.domain.model

enum class ReportFormat {
    PDF,
    JSON,
}

data class GeneratedReport(
    val format: ReportFormat,
    val uri: String,
    val sizeBytes: Long,
    val fileName: String,
    val mimeType: String,
)

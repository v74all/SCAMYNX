package com.v7lthronyx.scamynx.data.util

data class ApiCredentials(
    val virusTotalApiKey: String?,
    val googleSafeBrowsingApiKey: String?,
    val urlScanApiKey: String?,
    val telemetryEndpoint: String?,
    val openAiApiKey: String?,
    val groqApiKey: String?,
    val openRouterApiKey: String?,
    val huggingFaceApiKey: String?,
) {
    val isTelemetryConfigured: Boolean
        get() = !telemetryEndpoint.isNullOrBlank()

    val isAiConfigured: Boolean
        get() = !groqApiKey.isNullOrBlank() || 
                !openRouterApiKey.isNullOrBlank() || 
                !huggingFaceApiKey.isNullOrBlank() ||
                !openAiApiKey.isNullOrBlank()
}

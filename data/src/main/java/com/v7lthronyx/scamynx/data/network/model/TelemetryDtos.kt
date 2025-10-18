package com.v7lthronyx.scamynx.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelemetryEventDto(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("event_type")
    val eventType: String,
    val timestamp: String,
    val payload: Map<String, String> = emptyMap(),
    @SerialName("app_version")
    val appVersion: String? = null,
    @SerialName("device_info")
    val deviceInfo: DeviceInfoDto? = null,
)

@Serializable
data class DeviceInfoDto(
    val manufacturer: String? = null,
    val model: String? = null,
    @SerialName("android_version")
    val androidVersion: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null,
)

@Serializable
data class TelemetryBatchRequestDto(
    val events: List<TelemetryEventDto>,
    @SerialName("batch_id")
    val batchId: String,
)

@Serializable
data class TelemetryResponseDto(
    val status: String,
    val message: String? = null,
    @SerialName("processed_events")
    val processedEvents: Int? = null,
    @SerialName("failed_events")
    val failedEvents: Int? = null,
)
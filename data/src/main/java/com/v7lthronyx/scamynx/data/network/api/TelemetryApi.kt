package com.v7lthronyx.scamynx.data.network.api

import com.v7lthronyx.scamynx.data.network.model.TelemetryBatchRequestDto
import com.v7lthronyx.scamynx.data.network.model.TelemetryEventDto
import com.v7lthronyx.scamynx.data.network.model.TelemetryResponseDto
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface TelemetryApi {
    @Headers("Content-Type: application/json")
    @POST("v1/telemetry/event")
    suspend fun sendSingleEvent(@Body event: TelemetryEventDto): TelemetryResponseDto

    @Headers("Content-Type: application/json")
    @POST("v1/telemetry/batch")
    suspend fun sendBatchEvents(@Body batch: TelemetryBatchRequestDto): TelemetryResponseDto
}

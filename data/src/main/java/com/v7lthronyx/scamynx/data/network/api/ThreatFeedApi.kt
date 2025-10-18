package com.v7lthronyx.scamynx.data.network.api

import com.v7lthronyx.scamynx.data.network.model.ThreatFeedResponseDto
import retrofit2.http.GET

interface ThreatFeedApi {
    @GET("v1/threat-feed/latest")
    suspend fun fetchLatest(): ThreatFeedResponseDto
}

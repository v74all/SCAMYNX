package com.v7lthronyx.scamynx.data.network.api

import com.v7lthronyx.scamynx.data.network.model.ThreatFoxSearchRequestDto
import com.v7lthronyx.scamynx.data.network.model.ThreatFoxSearchResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ThreatFoxApi {
    @POST("v1/")
    suspend fun search(@Body request: ThreatFoxSearchRequestDto): ThreatFoxSearchResponseDto
}

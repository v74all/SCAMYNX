package com.v7lthronyx.scamynx.data.network.api

import com.v7lthronyx.scamynx.data.network.model.PhishStatsRecordDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PhishStatsApi {
    @GET("api/phishing")
    suspend fun search(
        @Query("url") url: String,
        @Query("format") format: String = "json",
    ): List<PhishStatsRecordDto>
}

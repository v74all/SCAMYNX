package com.v7lthronyx.scamynx.data.network.api

import com.v7lthronyx.scamynx.data.network.model.GoogleSafeBrowsingRequestDto
import com.v7lthronyx.scamynx.data.network.model.GoogleSafeBrowsingResponseDto
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GoogleSafeBrowsingApi {
    @Headers("Content-Type: application/json")
    @POST("v4/threatMatches:find")
    suspend fun findThreats(
        @Body request: GoogleSafeBrowsingRequestDto,
    ): GoogleSafeBrowsingResponseDto
}

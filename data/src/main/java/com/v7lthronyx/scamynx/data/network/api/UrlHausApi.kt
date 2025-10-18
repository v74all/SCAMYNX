package com.v7lthronyx.scamynx.data.network.api

import com.v7lthronyx.scamynx.data.network.model.UrlHausLookupRequestDto
import com.v7lthronyx.scamynx.data.network.model.UrlHausLookupResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface UrlHausApi {
    @POST("v1/url/")
    suspend fun lookup(@Body request: UrlHausLookupRequestDto): UrlHausLookupResponseDto
}

package com.v7lthronyx.scamynx.data.network.api

import com.v7lthronyx.scamynx.data.network.model.UrlScanSubmitRequestDto
import com.v7lthronyx.scamynx.data.network.model.UrlScanSubmitResponseDto
import com.v7lthronyx.scamynx.data.network.model.UrlScanResultDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface UrlScanApi {
    @Headers("Content-Type: application/json")
    @POST("api/v1/scan")
    suspend fun submitUrl(@Body request: UrlScanSubmitRequestDto): UrlScanSubmitResponseDto

    @GET("api/v1/result/{uuid}")
    suspend fun fetchResult(@Path("uuid") uuid: String): UrlScanResultDto
}

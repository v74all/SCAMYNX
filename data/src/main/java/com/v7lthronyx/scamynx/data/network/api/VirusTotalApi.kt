package com.v7lthronyx.scamynx.data.network.api

import com.v7lthronyx.scamynx.data.network.model.VirusTotalReportDto
import com.v7lthronyx.scamynx.data.network.model.VirusTotalSubmitRequestDto
import com.v7lthronyx.scamynx.data.network.model.VirusTotalSubmitResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface VirusTotalApi {
    @Headers("Accept: application/json")
    @POST("v3/urls")
    suspend fun submitUrl(@Body request: VirusTotalSubmitRequestDto): VirusTotalSubmitResponseDto

    @Headers("Accept: application/json")
    @GET("v3/urls/{id}")
    suspend fun fetchReport(@Path("id") id: String): VirusTotalReportDto
}

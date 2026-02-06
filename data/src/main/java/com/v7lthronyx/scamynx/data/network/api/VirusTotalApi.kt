package com.v7lthronyx.scamynx.data.network.api

import com.v7lthronyx.scamynx.data.network.model.VirusTotalFileReportDto
import com.v7lthronyx.scamynx.data.network.model.VirusTotalIpReportDto
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

    /**
     * Get file report by hash (MD5, SHA-1, or SHA-256)
     * https://docs.virustotal.com/reference/file-info
     */
    @Headers("Accept: application/json")
    @GET("v3/files/{hash}")
    suspend fun getFileReport(@Path("hash") hash: String): VirusTotalFileReportDto

    /**
     * Get IP address report
     * https://docs.virustotal.com/reference/ip-info
     */
    @Headers("Accept: application/json")
    @GET("v3/ip_addresses/{ip}")
    suspend fun getIpReport(@Path("ip") ip: String): VirusTotalIpReportDto
}

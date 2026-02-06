package com.v7lthronyx.scamynx.data.network.api

import com.v7lthronyx.scamynx.data.network.supabase.SupabaseThreatFeedService.SupabaseThreatIndicatorRecord
import com.v7lthronyx.scamynx.data.network.supabase.SupabaseThreatFeedService.SupabaseThreatIndicatorUpsert
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseRestApi {

    @GET("threat_indicators")
    suspend fun fetchThreatIndicators(
        @Query("select") select: String = "*",
        @Query("order") order: String = "fetched_at.desc",
        @Query("limit") limit: Int = 200,
    ): List<SupabaseThreatIndicatorRecord>

    @Headers(
        "Prefer: resolution=merge-duplicates",
        "Prefer: return=representation",
    )
    @POST("threat_indicators")
    suspend fun upsertThreatIndicators(
        @Body payload: List<SupabaseThreatIndicatorUpsert>,
    ): List<SupabaseThreatIndicatorRecord>
}

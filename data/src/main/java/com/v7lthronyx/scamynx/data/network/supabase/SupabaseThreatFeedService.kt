package com.v7lthronyx.scamynx.data.network.supabase

import android.util.Log
import com.v7lthronyx.scamynx.data.network.api.SupabaseRestApi
import com.v7lthronyx.scamynx.data.network.model.ThreatFeedResponseDto
import com.v7lthronyx.scamynx.data.network.model.ThreatIndicatorDto
import com.v7lthronyx.scamynx.data.util.SupabaseCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

private const val TAG = "SupabaseThreatFeed"
private const val AI_FUNCTION = "threat-intel-ai"
private const val FUNCTIONS_PATH = "functions/v1/"

class SupabaseThreatFeedService(
    private val api: SupabaseRestApi?,
    private val httpClient: OkHttpClient?,
    private val credentials: SupabaseCredentials?,
    private val json: Json,
) {

    suspend fun fetchLatest(limit: Int = 200): ThreatFeedResponseDto? = withContext(Dispatchers.IO) {
        val api = api ?: return@withContext null
        runCatching {
            val records = api.fetchThreatIndicators(limit = limit)
            ThreatFeedResponseDto(
                indicators = records.map { it.toDto() },
                generatedAt = records.firstOrNull()?.fetchedAt,
            )
        }.onFailure { error ->
            Log.w(TAG, "Supabase fetch failed", error)
        }.getOrNull()
    }

    suspend fun upsertIndicators(indicators: List<ThreatIndicatorDto>) = withContext(Dispatchers.IO) {
        val api = api ?: return@withContext
        if (indicators.isEmpty()) return@withContext
        val payload = indicators.map { SupabaseThreatIndicatorUpsert.fromDto(it) }
        runCatching {
            api.upsertThreatIndicators(payload)
        }.onFailure { error ->
            Log.w(TAG, "Supabase upsert failed", error)
        }
    }

    suspend fun requestAiVerdict(payload: AiThreatAnalysisRequest): AiThreatAnalysisResponse? = withContext(Dispatchers.IO) {
        val httpClient = httpClient ?: return@withContext null
        val credentials = credentials ?: return@withContext null
        val baseUrl = credentials.url.ensureTrailingSlash() + FUNCTIONS_PATH + AI_FUNCTION
        val body = json.encodeToString(payload)
            .toRequestBody("application/json".toMediaType())
        val authToken = credentials.functionJwt ?: credentials.anonKey
        val request = Request.Builder()
            .url(baseUrl)
            .post(body)
            .header("apikey", credentials.anonKey)
            .header("Authorization", "Bearer $authToken")
            .header("Content-Type", "application/json")
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                response.ensureSuccess()
                val text = response.body?.string().orEmpty()
                json.decodeFromString(AiThreatAnalysisResponse.serializer(), text)
            }
        }.onFailure { error ->
            Log.w(TAG, "Supabase AI invocation failed", error)
        }.getOrNull()
    }

    @Serializable
    data class SupabaseThreatIndicatorRecord(
        @SerialName("indicator_id")
        val indicatorId: String,
        val url: String,
        @SerialName("risk_score")
        val riskScore: Double,
        val tags: List<String> = emptyList(),
        @SerialName("last_seen")
        val lastSeen: String? = null,
        val source: String,
        @SerialName("fetched_at")
        val fetchedAt: String? = null,
    ) {
        fun toDto(): ThreatIndicatorDto = ThreatIndicatorDto(
            id = indicatorId,
            url = url,
            riskScore = riskScore,
            tags = tags,
            lastSeen = lastSeen,
            source = source,
        )
    }

    @Serializable
    data class SupabaseThreatIndicatorUpsert(
        @SerialName("indicator_id")
        val indicatorId: String,
        val url: String,
        @SerialName("risk_score")
        val riskScore: Double,
        val tags: List<String> = emptyList(),
        @SerialName("last_seen")
        val lastSeen: String? = null,
        val source: String,
    ) {
        companion object {
            fun fromDto(dto: ThreatIndicatorDto) = SupabaseThreatIndicatorUpsert(
                indicatorId = dto.id,
                url = dto.url,
                riskScore = dto.riskScore,
                tags = dto.tags,
                lastSeen = dto.lastSeen,
                source = dto.source,
            )
        }
    }

    @Serializable
    data class AiThreatAnalysisRequest(
        val url: String,
        @SerialName("screenshot_base64")
        val screenshotBase64: String? = null,
        @SerialName("network_findings")
        val networkFindings: List<String> = emptyList(),
        @SerialName("model_hint")
        val modelHint: String? = null,
    )

    @Serializable
    data class AiThreatAnalysisResponse(
        val summary: String,
        val verdict: String,
        val confidence: Double?,
        val mitigation: List<String> = emptyList(),
    )
}

private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

@Throws(IOException::class)
private fun Response.ensureSuccess() {
    if (!isSuccessful) {
        throw IOException("Supabase request failed with HTTP $code")
    }
}

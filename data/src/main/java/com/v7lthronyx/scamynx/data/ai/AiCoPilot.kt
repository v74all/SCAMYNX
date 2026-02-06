package com.v7lthronyx.scamynx.data.ai

import android.util.Log
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.di.ThreatIntelJson
import com.v7lthronyx.scamynx.data.util.ApiCredentials
import com.v7lthronyx.scamynx.domain.model.MlReport
import com.v7lthronyx.scamynx.domain.model.NetworkReport
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AiCoPilot"

// API Endpoints
private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
private const val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
private const val HUGGINGFACE_API_URL = "https://router.huggingface.co/v1/chat/completions"
private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"

// Models
private const val GROQ_MODEL = "llama-3.3-70b-versatile"
private const val HUGGINGFACE_MODEL = "zai-org/GLM-4.7-Flash"
private const val OPENAI_MODEL = "gpt-4o-mini"

// OpenRouter Free Models - با fallback برای اطمینان بیشتر
private val OPENROUTER_FREE_MODELS = listOf(
    "nvidia/nemotron-3-nano-30b-a3b:free",      // 30B با reasoning - 256K context
    "nvidia/nemotron-nano-9b-v2:free",           // 9B با reasoning - 128K context
    "arcee-ai/trinity-large-preview:free",       // فارسی خوب - 131K context
    "google/gemma-3n-e2b-it:free",               // Google Gemma - 8K context
)

/**
 * Unified AI Co-Pilot that supports multiple providers with fallback:
 * 1. Groq (Llama 3.3 70B) - Primary (fast & free)
 * 2. OpenRouter (NVIDIA Nemotron, Trinity, Gemma) - Secondary (free models)
 * 3. Hugging Face (GLM-4.7-Flash) - Tertiary (free)
 * 4. OpenAI (GPT-4o-mini) - Final fallback
 */
@Singleton
class AiCoPilot @Inject constructor(
    private val httpClient: OkHttpClient,
    private val credentials: ApiCredentials,
    @ThreatIntelJson private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun analyzeUrl(
        url: String,
        vendorVerdicts: List<VendorVerdict>,
        networkReport: NetworkReport?,
        mlReport: MlReport?,
    ): VendorVerdict? = withContext(ioDispatcher) {
        if (!credentials.isAiConfigured) {
            Log.d(TAG, "AI not configured, skipping analysis")
            return@withContext null
        }

        val prompt = buildAnalysisPrompt(url, vendorVerdicts, networkReport, mlReport)

        // Try Groq first (primary - fastest)
        val groqResult = credentials.groqApiKey?.takeIf { it.isNotBlank() }?.let { apiKey ->
            runCatching {
                callChatApi(
                    apiUrl = GROQ_API_URL,
                    apiKey = apiKey,
                    model = GROQ_MODEL,
                    prompt = prompt,
                    providerName = "Groq"
                )
            }.onFailure { error ->
                Log.w(TAG, "Groq analysis failed, trying OpenRouter", error)
            }.getOrNull()
        }

        if (groqResult != null) {
            return@withContext groqResult
        }

        // Try OpenRouter (secondary - free models with fallback)
        val openRouterResult = credentials.openRouterApiKey?.takeIf { it.isNotBlank() }?.let { apiKey ->
            tryOpenRouterModels(apiKey, prompt)
        }

        if (openRouterResult != null) {
            return@withContext openRouterResult
        }

        // Try Hugging Face (tertiary - GLM-4.7-Flash)
        val huggingFaceResult = credentials.huggingFaceApiKey?.takeIf { it.isNotBlank() }?.let { apiKey ->
            runCatching {
                callChatApi(
                    apiUrl = HUGGINGFACE_API_URL,
                    apiKey = apiKey,
                    model = HUGGINGFACE_MODEL,
                    prompt = prompt,
                    providerName = "HuggingFace"
                )
            }.onFailure { error ->
                Log.w(TAG, "HuggingFace analysis failed, trying OpenAI", error)
            }.getOrNull()
        }

        if (huggingFaceResult != null) {
            return@withContext huggingFaceResult
        }

        // Fallback to OpenAI
        val openAiResult = credentials.openAiApiKey?.takeIf { it.isNotBlank() }?.let { apiKey ->
            runCatching {
                callChatApi(
                    apiUrl = OPENAI_API_URL,
                    apiKey = apiKey,
                    model = OPENAI_MODEL,
                    prompt = prompt,
                    providerName = "OpenAI"
                )
            }.onFailure { error ->
                Log.w(TAG, "OpenAI analysis failed", error)
            }.getOrNull()
        }

        openAiResult
    }

    private fun buildAnalysisPrompt(
        url: String,
        vendorVerdicts: List<VendorVerdict>,
        networkReport: NetworkReport?,
        mlReport: MlReport?,
    ): String {
        val heuristicSignals = vendorVerdicts
            .firstOrNull { it.provider == Provider.LOCAL_HEURISTIC }
            ?.details
            ?.get("signals")
            ?.takeIf { it.isNotBlank() }

        val vendorSummary = vendorVerdicts.joinToString(separator = "; ") { verdict ->
            val status = verdict.status.name.lowercase()
            val score = "%.2f".format(verdict.score.coerceIn(0.0, 1.0))
            "${verdict.provider.name.lowercase()}:$status:$score"
        }.take(280)

        val networkSummary = buildString {
            networkReport?.let { network ->
                append("tls=${network.tlsVersion ?: "unknown"}, ")
                append("cert=${network.certValid ?: "unknown"}, ")
                if (network.headers.isNotEmpty()) {
                    append("headers=")
                    append(network.headers.keys.take(3).joinToString())
                }
            }
        }.ifBlank { null }

        val mlSummary = mlReport?.probability?.let { prob ->
            "ml_probability=${"%.2f".format(prob.coerceIn(0.0, 1.0))}"
        }

        return buildString {
            appendLine("You are a threat analyst. Combine on-device ML signals and heuristics to classify the URL.")
            appendLine("Return ONLY compact JSON with fields: verdict (malicious|suspicious|clean), score (0-1), reasons (array of short phrases), actions (array of user suggestions).")
            appendLine("Context:")
            appendLine("- url: $url")
            appendLine("- vendor_signals: $vendorSummary")
            heuristicSignals?.let { appendLine("- heuristic_signals: $it") }
            mlSummary?.let { appendLine("- ml: $mlSummary") }
            networkSummary?.let { appendLine("- network: $networkSummary") }
            append("Focus on phishing/scam risk. Keep reasons concise.")
        }
    }

    private fun callChatApi(
        apiUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        providerName: String,
    ): VendorVerdict? {
        val requestPayload = ChatCompletionRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = "You are a concise cybersecurity co-pilot."),
                ChatMessage(role = "user", content = prompt),
            ),
            temperature = 0.15,
            maxTokens = 260,
        )

        val body = json.encodeToString(requestPayload)
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("$providerName responded with HTTP ${response.code}")
            }
            val payload = response.body?.string().orEmpty()
            val completion = json.decodeFromString(ChatCompletionResponse.serializer(), payload)
            val content = completion.choices.firstOrNull()?.message?.content.orEmpty()
            Log.d(TAG, "$providerName analysis successful")
            parseAiVerdict(content)?.toVendorVerdict()
        }
    }

    /**
     * Try multiple free OpenRouter models with fallback.
     * Goes through the list until one succeeds.
     */
    private fun tryOpenRouterModels(apiKey: String, prompt: String): VendorVerdict? {
        for ((index, model) in OPENROUTER_FREE_MODELS.withIndex()) {
            val result = runCatching {
                callChatApi(
                    apiUrl = OPENROUTER_API_URL,
                    apiKey = apiKey,
                    model = model,
                    prompt = prompt,
                    providerName = "OpenRouter/$model"
                )
            }.onFailure { error ->
                val nextModel = OPENROUTER_FREE_MODELS.getOrNull(index + 1)
                if (nextModel != null) {
                    Log.w(TAG, "OpenRouter model '$model' failed, trying '$nextModel'", error)
                } else {
                    Log.w(TAG, "OpenRouter model '$model' failed, no more models to try", error)
                }
            }.getOrNull()

            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun parseAiVerdict(content: String): AiVerdictPayload? {
        val sanitized = content.trim()
        val jsonPayload = sanitized
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return runCatching {
            json.decodeFromString(AiVerdictPayload.serializer(), jsonPayload)
        }.getOrNull()
    }

    private fun AiVerdictPayload.toVendorVerdict(): VendorVerdict {
        val status = when (verdict.lowercase()) {
            "malicious" -> VerdictStatus.MALICIOUS
            "suspicious" -> VerdictStatus.SUSPICIOUS
            "clean" -> VerdictStatus.CLEAN
            else -> VerdictStatus.UNKNOWN
        }
        val rawScore = score ?: when (status) {
            VerdictStatus.MALICIOUS -> 0.82
            VerdictStatus.SUSPICIOUS -> 0.55
            VerdictStatus.CLEAN -> 0.1
            else -> 0.0
        }
        val safeScore = rawScore.coerceIn(0.0, 1.0)
        val shortReasons = reasons.take(3).joinToString(separator = " • ").ifBlank { null }
        val shortActions = actions.take(2).joinToString(separator = " • ").ifBlank { null }

        val details = buildMap {
            shortReasons?.let { put("ai_reasons", it) }
            shortActions?.let { put("ai_actions", it) }
            put("ai_verdict", verdict.lowercase())
        }

        return VendorVerdict(
            provider = Provider.CHAT_GPT,
            status = status,
            score = safeScore,
            details = details,
        )
    }
}

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int = 256,
    val temperature: Double = 0.2,
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
private data class ChatChoice(
    val message: ChatMessage? = null,
)

@Serializable
private data class AiVerdictPayload(
    val verdict: String,
    val score: Double? = null,
    val reasons: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
)

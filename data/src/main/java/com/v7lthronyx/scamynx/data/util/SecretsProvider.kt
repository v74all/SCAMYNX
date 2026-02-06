package com.v7lthronyx.scamynx.data.util

import com.v7lthronyx.scamynx.data.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecretsProvider @Inject constructor() {

    val apiCredentials: ApiCredentials
        get() = ApiCredentials(
            virusTotalApiKey = BuildConfig.VIRUSTOTAL_API_KEY.normalizeSecret(),
            googleSafeBrowsingApiKey = BuildConfig.GOOGLE_SAFE_BROWSING_API_KEY.normalizeSecret(),
            urlScanApiKey = BuildConfig.URLSCAN_API_KEY.normalizeSecret(),
            telemetryEndpoint = BuildConfig.SCAMYNX_TELEMETRY_ENDPOINT.normalizeSecret(),
            openAiApiKey = BuildConfig.OPENAI_API_KEY.normalizeSecret(),
            groqApiKey = BuildConfig.GROQ_API_KEY.normalizeSecret(),
            openRouterApiKey = BuildConfig.OPENROUTER_API_KEY.normalizeSecret(),
            huggingFaceApiKey = BuildConfig.HUGGINGFACE_API_KEY.normalizeSecret(),
        )

    val supabaseCredentials: SupabaseCredentials?
        get() {
            val url = BuildConfig.SUPABASE_URL.normalizeSecret() ?: return null
            val anonKey = BuildConfig.SUPABASE_ANON_KEY.normalizeSecret() ?: return null
            return SupabaseCredentials(
                url = url,
                anonKey = anonKey,
                functionJwt = BuildConfig.SUPABASE_FUNCTION_JWT.normalizeSecret(),
            )
        }
}

private fun String?.normalizeSecret(): String? = this
    ?.takeUnless { value ->
        value.isBlank() || value.equals("null", ignoreCase = true) || value.startsWith("dummy", ignoreCase = true)
    }

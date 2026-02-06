package com.v7lthronyx.scamynx.data.di

import android.os.Build
import com.v7lthronyx.scamynx.data.BuildConfig
import com.v7lthronyx.scamynx.data.network.api.GoogleSafeBrowsingApi
import com.v7lthronyx.scamynx.data.network.api.PhishStatsApi
import com.v7lthronyx.scamynx.data.network.api.TelemetryApi
import com.v7lthronyx.scamynx.data.network.api.ThreatFoxApi
import com.v7lthronyx.scamynx.data.network.api.UrlHausApi
import com.v7lthronyx.scamynx.data.network.api.UrlScanApi
import com.v7lthronyx.scamynx.data.network.api.VirusTotalApi
import com.v7lthronyx.scamynx.data.network.interceptor.ApiKeyInterceptor
import com.v7lthronyx.scamynx.data.network.interceptor.RateLimitInterceptor
import com.v7lthronyx.scamynx.data.network.interceptor.RetryInterceptor
import com.v7lthronyx.scamynx.data.network.interceptor.UserAgentInterceptor
import com.v7lthronyx.scamynx.data.util.ApiCredentials
import com.v7lthronyx.scamynx.data.util.SecretsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

private const val VIRUS_TOTAL_BASE_URL = "https://www.virustotal.com/api/"
private const val GOOGLE_SAFE_BROWSING_BASE_URL = "https://safebrowsing.googleapis.com/"
private const val URLSCAN_BASE_URL = "https://urlscan.io/"
private const val URLHAUS_BASE_URL = "https://urlhaus-api.abuse.ch/"
private const val PHISH_STATS_BASE_URL = "https://phishstats.info:2096/"
private const val THREAT_FOX_BASE_URL = "https://threatfox-api.abuse.ch/"
private const val USER_AGENT_VERSION = "0.1.0"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiCredentials(secretsProvider: SecretsProvider): ApiCredentials =
        secretsProvider.apiCredentials

        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    @ThreatIntelJson
    fun provideThreatIntelJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.BASIC
        }
    }

    @Provides
    @Singleton
    fun provideUserAgentInterceptor(): UserAgentInterceptor {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val sdk = Build.VERSION.SDK_INT
        val userAgent = "SCAMYNX/$USER_AGENT_VERSION ($manufacturer $model; SDK $sdk)"
        return UserAgentInterceptor(userAgent)
    }

    @Provides
    @Singleton
    fun provideRateLimitInterceptor(): RateLimitInterceptor = RateLimitInterceptor(minimumIntervalMillis = 500)

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor = RetryInterceptor(maxRetries = 3, initialDelayMs = 1000L)

    @Provides
    @Singleton
    fun provideBaseOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        userAgentInterceptor: UserAgentInterceptor,
        rateLimitInterceptor: RateLimitInterceptor,
        retryInterceptor: RetryInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(rateLimitInterceptor)
        .addInterceptor(retryInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @VirusTotalClient
    fun provideVirusTotalRetrofit(
        baseClient: OkHttpClient,
        credentials: ApiCredentials,
        @ThreatIntelJson json: Json,
    ): Retrofit = buildRetrofit(
        baseClient = baseClient,
        additionalInterceptors = listOf(
            ApiKeyInterceptor("x-apikey") { credentials.virusTotalApiKey },
        ),
        baseUrl = VIRUS_TOTAL_BASE_URL,
        json = json,
    )

    @Provides
    @Singleton
    @GoogleSafeBrowsingClient
    fun provideGoogleSafeBrowsingRetrofit(
        baseClient: OkHttpClient,
        credentials: ApiCredentials,
        @ThreatIntelJson json: Json,
    ): Retrofit = buildRetrofit(
        baseClient = baseClient,
        additionalInterceptors = listOf(
            ApiKeyInterceptor("X-Goog-Api-Key") { credentials.googleSafeBrowsingApiKey },
        ),
        baseUrl = GOOGLE_SAFE_BROWSING_BASE_URL,
        json = json,
    )

    @Provides
    @Singleton
    @UrlScanClient
    fun provideUrlScanRetrofit(
        baseClient: OkHttpClient,
        credentials: ApiCredentials,
        @ThreatIntelJson json: Json,
    ): Retrofit = buildRetrofit(
        baseClient = baseClient,
        additionalInterceptors = listOf(
            ApiKeyInterceptor("API-Key") { credentials.urlScanApiKey },
        ),
        baseUrl = URLSCAN_BASE_URL,
        json = json,
    )

    @Provides
    @Singleton
    @UrlHausClient
    fun provideUrlHausRetrofit(
        baseClient: OkHttpClient,
        @ThreatIntelJson json: Json,
    ): Retrofit = buildRetrofit(
        baseClient = baseClient,
        additionalInterceptors = emptyList(),
        baseUrl = URLHAUS_BASE_URL,
        json = json,
    )

    @Provides
    @Singleton
    @PhishStatsClient
    fun providePhishStatsRetrofit(
        baseClient: OkHttpClient,
        @ThreatIntelJson json: Json,
    ): Retrofit = buildRetrofit(
        baseClient = baseClient,
        additionalInterceptors = emptyList(),
        baseUrl = PHISH_STATS_BASE_URL,
        json = json,
    )

    @Provides
    @Singleton
    @ThreatFoxClient
    fun provideThreatFoxRetrofit(
        baseClient: OkHttpClient,
        @ThreatIntelJson json: Json,
    ): Retrofit = buildRetrofit(
        baseClient = baseClient,
        additionalInterceptors = emptyList(),
        baseUrl = THREAT_FOX_BASE_URL,
        json = json,
    )

    @Provides
    @Singleton
    fun provideVirusTotalApi(@VirusTotalClient retrofit: Retrofit): VirusTotalApi = retrofit.create(VirusTotalApi::class.java)

    @Provides
    @Singleton
    fun provideGoogleSafeBrowsingApi(@GoogleSafeBrowsingClient retrofit: Retrofit): GoogleSafeBrowsingApi =
        retrofit.create(GoogleSafeBrowsingApi::class.java)

    @Provides
    @Singleton
    fun provideUrlScanApi(@UrlScanClient retrofit: Retrofit): UrlScanApi = retrofit.create(UrlScanApi::class.java)

    @Provides
    @Singleton
    fun provideUrlHausApi(@UrlHausClient retrofit: Retrofit): UrlHausApi = retrofit.create(UrlHausApi::class.java)

    @Provides
    @Singleton
    fun providePhishStatsApi(@PhishStatsClient retrofit: Retrofit): PhishStatsApi = retrofit.create(PhishStatsApi::class.java)

    @Provides
    @Singleton
    fun provideThreatFoxApi(@ThreatFoxClient retrofit: Retrofit): ThreatFoxApi = retrofit.create(ThreatFoxApi::class.java)
}

private fun buildRetrofit(
    baseClient: OkHttpClient,
    additionalInterceptors: List<Interceptor>,
    baseUrl: String,
    json: Json,
): Retrofit {
    val clientBuilder = baseClient.newBuilder()
    additionalInterceptors.forEach { clientBuilder.addInterceptor(it) }
    val contentType = "application/json".toMediaType()
    return Retrofit.Builder()
    .baseUrl(baseUrl.ensureTrailingSlash())
        .addConverterFactory(json.asConverterFactory(contentType))
        .client(clientBuilder.build())
        .build()
}

private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

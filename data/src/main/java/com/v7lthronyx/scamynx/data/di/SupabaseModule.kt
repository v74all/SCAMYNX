package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.network.api.SupabaseRestApi
import com.v7lthronyx.scamynx.data.network.supabase.SupabaseThreatFeedService
import com.v7lthronyx.scamynx.data.util.SecretsProvider
import com.v7lthronyx.scamynx.data.util.SupabaseCredentials
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

private const val SUPABASE_REST_PATH = "rest/v1/"
private const val SUPABASE_FUNCTIONS_PATH = "functions/v1/"

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseCredentials(secretsProvider: SecretsProvider): SupabaseCredentials? =
        secretsProvider.supabaseCredentials?.takeIf { it.isConfigured }

    @Provides
    @Singleton
    @SupabaseRestClient
    fun provideSupabaseOkHttpClient(
        baseClient: OkHttpClient,
        credentials: SupabaseCredentials?,
    ): OkHttpClient? {
        credentials ?: return null
        val authInterceptor = Interceptor { chain ->
            val authToken = credentials.functionJwt ?: credentials.anonKey
            val request = chain.request()
                .newBuilder()
                .header("apikey", credentials.anonKey)
                .header("Authorization", "Bearer $authToken")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        return baseClient.newBuilder()
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @SupabaseRetrofit
    fun provideSupabaseRetrofit(
        credentials: SupabaseCredentials?,
        @SupabaseRestClient client: OkHttpClient?,
        @ThreatIntelJson json: Json,
    ): Retrofit? {
        credentials ?: return null
        client ?: return null
        val contentType = "application/json".toMediaType()
        val baseUrl = credentials.url.ensureTrailingSlash() + SUPABASE_REST_PATH
        return Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun provideSupabaseRestApi(
        @SupabaseRetrofit retrofit: Retrofit?,
    ): SupabaseRestApi? = retrofit?.create(SupabaseRestApi::class.java)

    @Provides
    @Singleton
    fun provideSupabaseThreatFeedService(
        supabaseRestApi: SupabaseRestApi?,
        @SupabaseRestClient client: OkHttpClient?,
        credentials: SupabaseCredentials?,
        @ThreatIntelJson json: Json,
    ): SupabaseThreatFeedService = SupabaseThreatFeedService(
        api = supabaseRestApi,
        httpClient = client,
        credentials = credentials,
        json = json,
    )
}

private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

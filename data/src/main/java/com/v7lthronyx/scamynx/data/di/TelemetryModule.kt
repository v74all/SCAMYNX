package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.db.TelemetryEventDao
import com.v7lthronyx.scamynx.data.network.api.TelemetryApi
import com.v7lthronyx.scamynx.data.telemetry.DefaultTelemetryRepository
import com.v7lthronyx.scamynx.data.telemetry.TelemetryRepository
import com.v7lthronyx.scamynx.data.util.ApiCredentials
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.di.ThreatIntelJson
import com.v7lthronyx.scamynx.data.di.TelemetryClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TelemetryModule {

    @Provides
    @Singleton
    @TelemetryClient
    fun provideTelemetryRetrofit(
        baseClient: OkHttpClient,
        credentials: ApiCredentials,
        @ThreatIntelJson json: Json,
    ): Retrofit? {
        val endpoint = credentials.telemetryEndpoint
        if (endpoint.isNullOrBlank()) return null

        val contentType = "application/json".toMediaType()
        val baseUrl = if (endpoint.endsWith("/")) endpoint else "$endpoint/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(baseClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideTelemetryApi(@TelemetryClient retrofit: Retrofit?): TelemetryApi? =
        retrofit?.create(TelemetryApi::class.java)

    @Provides
    @Singleton
    fun provideTelemetryRepository(
        telemetryApi: TelemetryApi?,
        credentials: ApiCredentials,
        telemetryEventDao: TelemetryEventDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @ThreatIntelJson json: Json,
    ): TelemetryRepository = DefaultTelemetryRepository(
        telemetryApi = telemetryApi,
        credentials = credentials,
        telemetryEventDao = telemetryEventDao,
        ioDispatcher = ioDispatcher,
        json = json,
    )
}

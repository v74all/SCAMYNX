package com.v7lthronyx.scamynx.networksecurity.di

import com.v7lthronyx.scamynx.domain.service.NetworkSecurityAnalyzer
import com.v7lthronyx.scamynx.networksecurity.analyzer.NetworkSecurityAnalyzerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkSecurityBindModule {

    @Binds
    @Singleton
    abstract fun bindNetworkSecurityAnalyzer(impl: NetworkSecurityAnalyzerImpl): NetworkSecurityAnalyzer
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkSecurityProvideModule {

    @Provides
    @Singleton
    @NetworkSecurityClient
    fun provideNetworkSecurityClient(): OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
}

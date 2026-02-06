package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.wifisecurity.WifiSecurityAnalyzerImpl
import com.v7lthronyx.scamynx.domain.service.WifiSecurityAnalyzer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WifiSecurityModule {

    @Binds
    @Singleton
    abstract fun bindWifiSecurityAnalyzer(
        impl: WifiSecurityAnalyzerImpl,
    ): WifiSecurityAnalyzer
}

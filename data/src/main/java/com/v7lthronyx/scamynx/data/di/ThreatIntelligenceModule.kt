package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.threatintel.ThreatIntelligenceServiceImpl
import com.v7lthronyx.scamynx.domain.service.ThreatIntelligenceService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ThreatIntelligenceModule {

    @Binds
    @Singleton
    abstract fun bindThreatIntelligenceService(
        impl: ThreatIntelligenceServiceImpl,
    ): ThreatIntelligenceService
}

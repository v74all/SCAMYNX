package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.antiphishing.HeuristicAntiPhishingAnalyzer
import com.v7lthronyx.scamynx.domain.service.AntiPhishingAnalyzer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AntiPhishingModule {

    @Binds
    @Singleton
    abstract fun bindAntiPhishingAnalyzer(
        impl: HeuristicAntiPhishingAnalyzer,
    ): AntiPhishingAnalyzer
}

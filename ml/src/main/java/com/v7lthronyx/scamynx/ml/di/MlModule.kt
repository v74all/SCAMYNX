package com.v7lthronyx.scamynx.ml.di

import com.v7lthronyx.scamynx.domain.service.MlAnalyzer
import com.v7lthronyx.scamynx.ml.analyzer.MlAnalyzerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MlModule {

    @Binds
    @Singleton
    abstract fun bindMlAnalyzer(impl: MlAnalyzerImpl): MlAnalyzer
}

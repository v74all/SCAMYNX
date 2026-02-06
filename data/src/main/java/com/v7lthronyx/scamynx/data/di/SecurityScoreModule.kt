package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.securityscore.SecurityScoreCalculatorImpl
import com.v7lthronyx.scamynx.domain.service.SecurityScoreCalculator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityScoreModule {

    @Binds
    @Singleton
    abstract fun bindSecurityScoreCalculator(
        impl: SecurityScoreCalculatorImpl,
    ): SecurityScoreCalculator
}

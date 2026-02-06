package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.passwordsecurity.HibpPasswordBreachDataSource
import com.v7lthronyx.scamynx.data.passwordsecurity.PasswordBreachDataSource
import com.v7lthronyx.scamynx.data.passwordsecurity.PasswordSecurityAnalyzerImpl
import com.v7lthronyx.scamynx.data.socialengineering.SocialEngineeringAnalyzerImpl
import com.v7lthronyx.scamynx.domain.service.PasswordSecurityAnalyzer
import com.v7lthronyx.scamynx.domain.service.SocialEngineeringAnalyzer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PasswordSecurityModule {

    @Binds
    @Singleton
    abstract fun bindPasswordSecurityAnalyzer(
        impl: PasswordSecurityAnalyzerImpl,
    ): PasswordSecurityAnalyzer

    @Binds
    @Singleton
    abstract fun bindPasswordBreachDataSource(
        impl: HibpPasswordBreachDataSource,
    ): PasswordBreachDataSource

    @Binds
    @Singleton
    abstract fun bindSocialEngineeringAnalyzer(
        impl: SocialEngineeringAnalyzerImpl,
    ): SocialEngineeringAnalyzer
}

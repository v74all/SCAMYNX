package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.realtimeprotection.RealTimeProtectionServiceImpl
import com.v7lthronyx.scamynx.domain.service.RealTimeProtectionService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RealTimeProtectionModule {

    @Binds
    @Singleton
    abstract fun bindRealTimeProtectionService(
        impl: RealTimeProtectionServiceImpl,
    ): RealTimeProtectionService
}

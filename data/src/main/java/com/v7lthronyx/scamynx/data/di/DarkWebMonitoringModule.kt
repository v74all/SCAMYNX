package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.darkwebmonitoring.DarkWebMonitoringRepositoryImpl
import com.v7lthronyx.scamynx.data.darkwebmonitoring.DarkWebMonitoringServiceImpl
import com.v7lthronyx.scamynx.domain.repository.DarkWebMonitoringRepository
import com.v7lthronyx.scamynx.domain.service.DarkWebMonitoringService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DarkWebMonitoringModule {

    @Binds
    @Singleton
    abstract fun bindDarkWebMonitoringService(
        impl: DarkWebMonitoringServiceImpl,
    ): DarkWebMonitoringService

    @Binds
    @Singleton
    abstract fun bindDarkWebMonitoringRepository(
        impl: DarkWebMonitoringRepositoryImpl,
    ): DarkWebMonitoringRepository
}

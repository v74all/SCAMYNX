package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.breachmonitoring.BreachMonitoringServiceImpl
import com.v7lthronyx.scamynx.domain.service.BreachMonitoringService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BreachMonitoringModule {

    @Binds
    @Singleton
    abstract fun bindBreachMonitoringService(
        impl: BreachMonitoringServiceImpl,
    ): BreachMonitoringService
}

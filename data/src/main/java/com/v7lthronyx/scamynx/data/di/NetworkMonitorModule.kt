package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.networkmonitor.NetworkMonitorServiceImpl
import com.v7lthronyx.scamynx.domain.service.NetworkMonitorService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitorService(
        impl: NetworkMonitorServiceImpl,
    ): NetworkMonitorService
}

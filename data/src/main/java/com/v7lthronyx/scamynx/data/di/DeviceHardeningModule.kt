package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.devicehardening.DeviceHardeningServiceImpl
import com.v7lthronyx.scamynx.domain.service.DeviceHardeningService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceHardeningModule {

    @Binds
    @Singleton
    abstract fun bindDeviceHardeningService(
        impl: DeviceHardeningServiceImpl,
    ): DeviceHardeningService
}

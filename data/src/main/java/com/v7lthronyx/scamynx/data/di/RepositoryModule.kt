package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.repository.ScanRepositoryImpl
import com.v7lthronyx.scamynx.data.repository.SettingsRepositoryImpl
import com.v7lthronyx.scamynx.domain.repository.ScanRepository
import com.v7lthronyx.scamynx.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScanRepository(impl: ScanRepositoryImpl): ScanRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

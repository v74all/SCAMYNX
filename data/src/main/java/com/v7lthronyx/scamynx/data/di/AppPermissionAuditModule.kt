package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.permissionaudit.AppPermissionAuditServiceImpl
import com.v7lthronyx.scamynx.domain.service.AppPermissionAuditService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppPermissionAuditModule {

    @Binds
    @Singleton
    abstract fun bindAppPermissionAuditService(
        impl: AppPermissionAuditServiceImpl,
    ): AppPermissionAuditService
}

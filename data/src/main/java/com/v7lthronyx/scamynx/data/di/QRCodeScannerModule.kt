package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.qrcode.QRCodeScannerServiceImpl
import com.v7lthronyx.scamynx.domain.service.QRCodeScannerService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class QRCodeScannerModule {

    @Binds
    @Singleton
    abstract fun bindQRCodeScannerService(
        impl: QRCodeScannerServiceImpl,
    ): QRCodeScannerService
}

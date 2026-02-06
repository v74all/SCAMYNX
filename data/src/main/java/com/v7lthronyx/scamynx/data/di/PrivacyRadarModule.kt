package com.v7lthronyx.scamynx.data.di

import com.v7lthronyx.scamynx.data.privacyradar.coordinator.DefaultPrivacyRadarCoordinator
import com.v7lthronyx.scamynx.data.privacyradar.coordinator.LifecycleSessionContextProvider
import com.v7lthronyx.scamynx.data.privacyradar.coordinator.PrivacyRadarConfig
import com.v7lthronyx.scamynx.data.privacyradar.coordinator.PrivacyRadarCoordinator
import com.v7lthronyx.scamynx.data.privacyradar.coordinator.SessionContextProvider
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyResourceType
import com.v7lthronyx.scamynx.data.privacyradar.source.PrivacyEventSource
import com.v7lthronyx.scamynx.data.privacyradar.source.impl.AntiPhishingLinkEventSource
import com.v7lthronyx.scamynx.data.privacyradar.source.impl.AppOpsPrivacyEventSource
import com.v7lthronyx.scamynx.data.privacyradar.source.impl.PackageManagerPrivacyEventSource
import com.v7lthronyx.scamynx.data.privacyradar.source.impl.SensorPrivacyEventSource
import com.v7lthronyx.scamynx.data.privacyradar.source.impl.UsageStatsPrivacyEventSource
import com.v7lthronyx.scamynx.data.privacyradar.source.impl.WifiSecurityEventSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrivacyRadarModule {

    @Provides
    @Singleton
    fun providePrivacyRadarConfig(): PrivacyRadarConfig = PrivacyRadarConfig(
        highPriorityResources = setOf(
            PrivacyResourceType.CAMERA,
            PrivacyResourceType.MICROPHONE,
            PrivacyResourceType.LOCATION,
            PrivacyResourceType.PHISHING_URL,
            PrivacyResourceType.WIFI_NETWORK,
        ),
        bufferCapacity = 256,
    )

    @Provides
    @Singleton
    fun providePrivacyEventSources(
        packageManagerSource: PackageManagerPrivacyEventSource,
        appOpsSource: AppOpsPrivacyEventSource,
        sensorPrivacyEventSource: SensorPrivacyEventSource,
        usageStatsPrivacyEventSource: UsageStatsPrivacyEventSource,
        antiPhishingLinkEventSource: AntiPhishingLinkEventSource,
        wifiSecurityEventSource: WifiSecurityEventSource,
    ): Set<PrivacyEventSource> = linkedSetOf(
        packageManagerSource,
        appOpsSource,
        sensorPrivacyEventSource,
        usageStatsPrivacyEventSource,
        antiPhishingLinkEventSource,
        wifiSecurityEventSource,
    )

    @Provides
    @Singleton
    fun providePrivacyRadarCoordinator(
        coordinator: DefaultPrivacyRadarCoordinator,
    ): PrivacyRadarCoordinator = coordinator

    @Provides
    @Singleton
    fun provideSessionContextProvider(
        provider: LifecycleSessionContextProvider,
    ): SessionContextProvider = provider
}

package com.v7lthronyx.scamynx.data.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class VirusTotalClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class GoogleSafeBrowsingClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UrlScanClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UrlHausClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PhishStatsClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThreatFoxClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThreatIntelJson

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class TelemetryClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class SupabaseRestClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class SupabaseRetrofit

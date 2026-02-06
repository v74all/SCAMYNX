package com.v7lthronyx.scamynx

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.v7lthronyx.scamynx.work.ScanWorkScheduler
import com.v7lthronyx.scamynx.domain.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class ScamynxApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var scanWorkScheduler: ScanWorkScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        applyPersistedLocale()
        scanWorkScheduler.scheduleThreatFeedSync()
        scanWorkScheduler.schedulePrivacyBaselineRefresh()
        configureTelemetryWork()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun applyPersistedLocale() {
        val localeTag = runBlocking { settingsRepository.settings.first().languageTag }
        
        if (localeTag.isBlank()) {
            // Auto-detect system language - check if Persian is available
            val systemLocale = java.util.Locale.getDefault()
            val detectedTag = when {
                systemLocale.language == "fa" -> "fa"
                systemLocale.toLanguageTag().startsWith("fa") -> "fa"
                else -> "en"
            }
            
            // Save the detected language
            runBlocking { settingsRepository.updateLanguage(detectedTag) }
            
            val locales = LocaleListCompat.forLanguageTags(detectedTag)
            AppCompatDelegate.setApplicationLocales(locales)
        } else {
            val locales = LocaleListCompat.forLanguageTags(localeTag)
            if (!locales.isEmpty) {
                AppCompatDelegate.setApplicationLocales(locales)
            }
        }
    }

    private fun configureTelemetryWork() {
        val telemetryOptIn = runBlocking { settingsRepository.settings.first().telemetryOptIn }
        scanWorkScheduler.updateTelemetrySync(telemetryOptIn)
    }
}

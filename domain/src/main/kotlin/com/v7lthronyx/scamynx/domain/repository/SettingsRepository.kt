package com.v7lthronyx.scamynx.domain.repository

import kotlinx.coroutines.flow.Flow

data class SettingsState(
    val languageTag: String = "", // Empty string means auto-detect from system
    val useDarkTheme: Boolean = true,
    val useDynamicColor: Boolean = true,
    val telemetryOptIn: Boolean = false,
)

interface SettingsRepository {
    val settings: Flow<SettingsState>
    suspend fun updateLanguage(tag: String)
    suspend fun updateTheme(useDarkTheme: Boolean)
    suspend fun updateDynamicColor(useDynamicColor: Boolean)
    suspend fun updateTelemetryOptIn(optIn: Boolean)
    suspend fun setOnboardingComplete()
    suspend fun isOnboardingComplete(): Boolean
}

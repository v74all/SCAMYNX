package com.v7lthronyx.scamynx.data.repository

import com.v7lthronyx.scamynx.data.preferences.SettingsDataSource
import com.v7lthronyx.scamynx.data.preferences.SettingsPreferences
import com.v7lthronyx.scamynx.domain.repository.SettingsRepository
import com.v7lthronyx.scamynx.domain.repository.SettingsState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataSource: SettingsDataSource,
) : SettingsRepository {

    override val settings: Flow<SettingsState> = dataSource.data.map { it.toDomain() }

    override suspend fun updateLanguage(tag: String) {
        dataSource.update { it.copy(languageTag = tag) }
    }

    override suspend fun updateTheme(useDarkTheme: Boolean) {
        dataSource.update { it.copy(useDarkTheme = useDarkTheme) }
    }

    override suspend fun updateDynamicColor(useDynamicColor: Boolean) {
        dataSource.update { it.copy(useDynamicColor = useDynamicColor) }
    }

    override suspend fun updateTelemetryOptIn(optIn: Boolean) {
        dataSource.update { it.copy(telemetryOptIn = optIn) }
    }
}

private fun SettingsPreferences.toDomain(): SettingsState = SettingsState(
    languageTag = languageTag,
    useDarkTheme = useDarkTheme,
    useDynamicColor = useDynamicColor,
    telemetryOptIn = telemetryOptIn,
)

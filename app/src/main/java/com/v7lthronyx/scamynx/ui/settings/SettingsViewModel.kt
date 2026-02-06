package com.v7lthronyx.scamynx.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.scamynx.domain.repository.SettingsRepository
import com.v7lthronyx.scamynx.domain.repository.SettingsState
import com.v7lthronyx.scamynx.work.ScanWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val DEFAULT_LANGUAGES = listOf("en", "fa")

data class SettingsUiState(
    val languageTag: String = "",
    val useDarkTheme: Boolean = true,
    val useDynamicColor: Boolean = true,
    val telemetryOptIn: Boolean = false,
    val supportedLanguages: List<String> = DEFAULT_LANGUAGES,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scanWorkScheduler: ScanWorkScheduler,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun onDarkThemeChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTheme(enabled) }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDynamicColor(enabled) }
    }

    fun onTelemetryChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTelemetryOptIn(enabled)
            scanWorkScheduler.updateTelemetrySync(enabled)
        }
    }

    fun onLanguageSelected(tag: String) {
        viewModelScope.launch { settingsRepository.updateLanguage(tag) }
    }
}

private fun SettingsState.toUiState(): SettingsUiState {
    val effectiveLanguageTag = if (languageTag.isBlank()) {
        // Auto-detect system language
        val systemLocale = Locale.getDefault()
        when {
            systemLocale.language == "fa" -> "fa"
            systemLocale.toLanguageTag().startsWith("fa") -> "fa"
            else -> "en"
        }
    } else {
        languageTag
    }
    
    return SettingsUiState(
        languageTag = effectiveLanguageTag,
        useDarkTheme = useDarkTheme,
        useDynamicColor = useDynamicColor,
        telemetryOptIn = telemetryOptIn,
        supportedLanguages = DEFAULT_LANGUAGES,
    )
}

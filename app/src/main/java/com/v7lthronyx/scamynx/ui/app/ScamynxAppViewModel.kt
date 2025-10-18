package com.v7lthronyx.scamynx.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.v7lthronyx.scamynx.domain.repository.SettingsRepository
import com.v7lthronyx.scamynx.domain.repository.SettingsState

data class ScamynxAppState(
    val isDarkTheme: Boolean = true,
    val currentLocale: Locale = Locale.getDefault(),
    val telemetryOptIn: Boolean = false,
    val dynamicColor: Boolean = true,
)

@HiltViewModel
class ScamynxAppViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScamynxAppState())
    val state: StateFlow<ScamynxAppState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.value = mapSettingsToState(settings)
            }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val newValue = !_state.value.isDarkTheme
            settingsRepository.updateTheme(newValue)
        }
    }

    fun setLocale(locale: Locale) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
        _state.update { current ->
            current.copy(currentLocale = locale)
        }
        viewModelScope.launch {
            settingsRepository.updateLanguage(locale.toLanguageTag())
        }
    }

    fun setTelemetryOptIn(optIn: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTelemetryOptIn(optIn)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDynamicColor(enabled)
        }
    }

    private fun mapSettingsToState(settings: SettingsState): ScamynxAppState {
        val locale = if (settings.languageTag.isBlank()) {
            // Auto-detect system language - prefer Persian if available
            val systemLocale = Locale.getDefault()
            when {
                systemLocale.language == "fa" -> Locale.forLanguageTag("fa")
                systemLocale.toLanguageTag().startsWith("fa") -> Locale.forLanguageTag("fa")
                else -> Locale.forLanguageTag("en")
            }
        } else {
            runCatching { Locale.forLanguageTag(settings.languageTag) }
                .getOrNull()
                ?.takeIf { it.language.isNotBlank() }
                ?: Locale.getDefault()
        }
        return ScamynxAppState(
            isDarkTheme = settings.useDarkTheme,
            currentLocale = locale,
            telemetryOptIn = settings.telemetryOptIn,
            dynamicColor = settings.useDynamicColor,
        )
    }
}

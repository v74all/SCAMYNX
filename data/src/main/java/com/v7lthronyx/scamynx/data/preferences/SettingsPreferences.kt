package com.v7lthronyx.scamynx.data.preferences

import kotlinx.serialization.Serializable

@Serializable
data class SettingsPreferences(
    val languageTag: String = "", // Empty string means auto-detect from system
    val useDarkTheme: Boolean = true,
    val useDynamicColor: Boolean = true,
    val telemetryOptIn: Boolean = false,
)

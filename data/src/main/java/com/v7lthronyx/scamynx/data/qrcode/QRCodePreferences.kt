package com.v7lthronyx.scamynx.data.qrcode

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.qrCodeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "qr_code_preferences"
)

/**
 * Manages QR code scanner preferences using DataStore.
 */
@Singleton
class QRCodePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val AUTO_SCAN_ENABLED = booleanPreferencesKey("auto_scan_enabled")
        val SOUND_FEEDBACK_ENABLED = booleanPreferencesKey("sound_feedback_enabled")
        val VIBRATION_FEEDBACK_ENABLED = booleanPreferencesKey("vibration_feedback_enabled")
    }

    suspend fun isAutoScanEnabled(): Boolean {
        return context.qrCodeDataStore.data
            .map { preferences -> preferences[Keys.AUTO_SCAN_ENABLED] ?: true }
            .first()
    }

    suspend fun setAutoScanEnabled(enabled: Boolean) {
        context.qrCodeDataStore.edit { preferences ->
            preferences[Keys.AUTO_SCAN_ENABLED] = enabled
        }
    }

    suspend fun isSoundFeedbackEnabled(): Boolean {
        return context.qrCodeDataStore.data
            .map { preferences -> preferences[Keys.SOUND_FEEDBACK_ENABLED] ?: true }
            .first()
    }

    suspend fun setSoundFeedbackEnabled(enabled: Boolean) {
        context.qrCodeDataStore.edit { preferences ->
            preferences[Keys.SOUND_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun isVibrationFeedbackEnabled(): Boolean {
        return context.qrCodeDataStore.data
            .map { preferences -> preferences[Keys.VIBRATION_FEEDBACK_ENABLED] ?: true }
            .first()
    }

    suspend fun setVibrationFeedbackEnabled(enabled: Boolean) {
        context.qrCodeDataStore.edit { preferences ->
            preferences[Keys.VIBRATION_FEEDBACK_ENABLED] = enabled
        }
    }
}

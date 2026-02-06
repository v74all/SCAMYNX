package com.v7lthronyx.scamynx.data.darkwebmonitoring

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.v7lthronyx.scamynx.domain.model.AlertPreferences
import com.v7lthronyx.scamynx.domain.model.ScanFrequency
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.darkWebDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dark_web_monitoring_prefs"
)

@Singleton
class DarkWebMonitoringPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    private object Keys {
        val IS_ENABLED = booleanPreferencesKey("is_enabled")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val SCAN_FREQUENCY = stringPreferencesKey("scan_frequency")
        val ALERT_PREFERENCES = stringPreferencesKey("alert_preferences")
        val LAST_SCAN = longPreferencesKey("last_scan")
        val NEXT_SCAN = longPreferencesKey("next_scan")
    }

    private val dataStore = context.darkWebDataStore

    suspend fun isEnabled(): Boolean {
        return dataStore.data.map { prefs ->
            prefs[Keys.IS_ENABLED] ?: false
        }.first()
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_ENABLED] = enabled
        }
    }

    suspend fun isPremium(): Boolean {
        return dataStore.data.map { prefs ->
            prefs[Keys.IS_PREMIUM] ?: false
        }.first()
    }

    suspend fun setPremium(premium: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = premium
        }
    }

    suspend fun getScanFrequency(): ScanFrequency {
        return dataStore.data.map { prefs ->
            prefs[Keys.SCAN_FREQUENCY]?.let { 
                try { ScanFrequency.valueOf(it) } catch (e: Exception) { ScanFrequency.DAILY }
            } ?: ScanFrequency.DAILY
        }.first()
    }

    suspend fun setScanFrequency(frequency: ScanFrequency) {
        dataStore.edit { prefs ->
            prefs[Keys.SCAN_FREQUENCY] = frequency.name
        }
    }

    suspend fun getAlertPreferences(): AlertPreferences {
        return dataStore.data.map { prefs ->
            prefs[Keys.ALERT_PREFERENCES]?.let {
                try { json.decodeFromString<AlertPreferences>(it) } catch (e: Exception) { null }
            } ?: AlertPreferences()
        }.first()
    }

    suspend fun setAlertPreferences(preferences: AlertPreferences) {
        dataStore.edit { prefs ->
            prefs[Keys.ALERT_PREFERENCES] = json.encodeToString(preferences)
        }
    }

    suspend fun getLastScan(): Long? {
        return dataStore.data.map { prefs ->
            prefs[Keys.LAST_SCAN]
        }.first()
    }

    suspend fun setLastScan(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_SCAN] = timestamp
        }
    }

    suspend fun getNextScan(): Long? {
        return dataStore.data.map { prefs ->
            prefs[Keys.NEXT_SCAN]
        }.first()
    }

    suspend fun setNextScan(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.NEXT_SCAN] = timestamp
        }
    }
}

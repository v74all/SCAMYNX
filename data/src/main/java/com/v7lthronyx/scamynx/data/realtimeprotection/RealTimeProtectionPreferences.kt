package com.v7lthronyx.scamynx.data.realtimeprotection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.ProtectionSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.protectionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "real_time_protection_preferences"
)

/**
 * Manages real-time protection settings using DataStore.
 */
@Singleton
class RealTimeProtectionPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val IS_ENABLED = booleanPreferencesKey("is_enabled")
        val LINK_PROTECTION = booleanPreferencesKey("link_protection")
        val APP_PROTECTION = booleanPreferencesKey("app_protection")
        val NETWORK_PROTECTION = booleanPreferencesKey("network_protection")
        val NOTIFICATION_PROTECTION = booleanPreferencesKey("notification_protection")
        val CLIPBOARD_PROTECTION = booleanPreferencesKey("clipboard_protection")
        val QR_CODE_PROTECTION = booleanPreferencesKey("qr_code_protection")
        val AUTO_BLOCK = booleanPreferencesKey("auto_block")
        val NOTIFY_ON_BLOCK = booleanPreferencesKey("notify_on_block")
        val NOTIFY_ON_WARNING = booleanPreferencesKey("notify_on_warning")
        val BLOCK_SEVERITY_THRESHOLD = stringPreferencesKey("block_severity_threshold")
        val SCAN_ON_WIFI_ONLY = booleanPreferencesKey("scan_on_wifi_only")
        val LOW_POWER_MODE = booleanPreferencesKey("low_power_mode")
        val ALLOWED_DOMAINS = stringSetPreferencesKey("allowed_domains")
        val BLOCKED_DOMAINS = stringSetPreferencesKey("blocked_domains")
        val ALLOWED_APPS = stringSetPreferencesKey("allowed_apps")
    }

    suspend fun getSettings(): ProtectionSettings {
        return context.protectionDataStore.data.map { prefs ->
            ProtectionSettings(
                isEnabled = prefs[Keys.IS_ENABLED] ?: true,
                linkProtection = prefs[Keys.LINK_PROTECTION] ?: true,
                appProtection = prefs[Keys.APP_PROTECTION] ?: true,
                networkProtection = prefs[Keys.NETWORK_PROTECTION] ?: true,
                notificationProtection = prefs[Keys.NOTIFICATION_PROTECTION] ?: true,
                clipboardProtection = prefs[Keys.CLIPBOARD_PROTECTION] ?: true,
                qrCodeProtection = prefs[Keys.QR_CODE_PROTECTION] ?: true,
                autoBlock = prefs[Keys.AUTO_BLOCK] ?: true,
                notifyOnBlock = prefs[Keys.NOTIFY_ON_BLOCK] ?: true,
                notifyOnWarning = prefs[Keys.NOTIFY_ON_WARNING] ?: true,
                blockSeverityThreshold = prefs[Keys.BLOCK_SEVERITY_THRESHOLD]?.let {
                    try { IssueSeverity.valueOf(it) } catch (e: Exception) { IssueSeverity.MEDIUM }
                } ?: IssueSeverity.MEDIUM,
                scanOnWifiOnly = prefs[Keys.SCAN_ON_WIFI_ONLY] ?: false,
                lowPowerMode = prefs[Keys.LOW_POWER_MODE] ?: false,
                allowedDomains = prefs[Keys.ALLOWED_DOMAINS]?.toList() ?: emptyList(),
                blockedDomains = prefs[Keys.BLOCKED_DOMAINS]?.toList() ?: emptyList(),
                allowedApps = prefs[Keys.ALLOWED_APPS]?.toList() ?: emptyList(),
            )
        }.first()
    }

    suspend fun updateSettings(settings: ProtectionSettings) {
        context.protectionDataStore.edit { prefs ->
            prefs[Keys.IS_ENABLED] = settings.isEnabled
            prefs[Keys.LINK_PROTECTION] = settings.linkProtection
            prefs[Keys.APP_PROTECTION] = settings.appProtection
            prefs[Keys.NETWORK_PROTECTION] = settings.networkProtection
            prefs[Keys.NOTIFICATION_PROTECTION] = settings.notificationProtection
            prefs[Keys.CLIPBOARD_PROTECTION] = settings.clipboardProtection
            prefs[Keys.QR_CODE_PROTECTION] = settings.qrCodeProtection
            prefs[Keys.AUTO_BLOCK] = settings.autoBlock
            prefs[Keys.NOTIFY_ON_BLOCK] = settings.notifyOnBlock
            prefs[Keys.NOTIFY_ON_WARNING] = settings.notifyOnWarning
            prefs[Keys.BLOCK_SEVERITY_THRESHOLD] = settings.blockSeverityThreshold.name
            prefs[Keys.SCAN_ON_WIFI_ONLY] = settings.scanOnWifiOnly
            prefs[Keys.LOW_POWER_MODE] = settings.lowPowerMode
            prefs[Keys.ALLOWED_DOMAINS] = settings.allowedDomains.toSet()
            prefs[Keys.BLOCKED_DOMAINS] = settings.blockedDomains.toSet()
            prefs[Keys.ALLOWED_APPS] = settings.allowedApps.toSet()
        }
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.protectionDataStore.edit { prefs ->
            prefs[Keys.IS_ENABLED] = enabled
        }
    }

    suspend fun resetToDefaults() {
        context.protectionDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

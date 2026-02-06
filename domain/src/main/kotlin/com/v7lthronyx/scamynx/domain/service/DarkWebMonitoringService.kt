package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.AlertPreferences
import com.v7lthronyx.scamynx.domain.model.AssetType
import com.v7lthronyx.scamynx.domain.model.CredentialIntelligence
import com.v7lthronyx.scamynx.domain.model.DarkWebAlert
import com.v7lthronyx.scamynx.domain.model.DarkWebExposure
import com.v7lthronyx.scamynx.domain.model.DarkWebMonitoringConfig
import com.v7lthronyx.scamynx.domain.model.DarkWebMonitoringReport
import com.v7lthronyx.scamynx.domain.model.IdentityProtectionStatus
import com.v7lthronyx.scamynx.domain.model.MonitoredAsset
import com.v7lthronyx.scamynx.domain.model.RemediationStatus
import com.v7lthronyx.scamynx.domain.model.ScanFrequency
import kotlinx.coroutines.flow.Flow

/**
 * Service for dark web monitoring functionality.
 * Monitors user assets (emails, phones, usernames, etc.) for exposure on the dark web.
 */
interface DarkWebMonitoringService {

    // ==================== Configuration ====================

    /**
     * Get the current monitoring configuration
     */
    suspend fun getMonitoringConfig(): DarkWebMonitoringConfig

    /**
     * Enable dark web monitoring
     */
    suspend fun enableMonitoring()

    /**
     * Disable dark web monitoring
     */
    suspend fun disableMonitoring()

    /**
     * Update scan frequency
     */
    suspend fun setScanFrequency(frequency: ScanFrequency)

    /**
     * Update alert preferences
     */
    suspend fun updateAlertPreferences(preferences: AlertPreferences)

    // ==================== Asset Management ====================

    /**
     * Add an asset to monitor (email, phone, username, etc.)
     */
    suspend fun addMonitoredAsset(type: AssetType, value: String): MonitoredAsset

    /**
     * Remove a monitored asset
     */
    suspend fun removeMonitoredAsset(assetId: String)

    /**
     * Get all monitored assets
     */
    suspend fun getMonitoredAssets(): List<MonitoredAsset>

    /**
     * Observe monitored assets as a flow
     */
    fun observeMonitoredAssets(): Flow<List<MonitoredAsset>>

    /**
     * Verify asset ownership (e.g., via email confirmation)
     */
    suspend fun requestAssetVerification(assetId: String)

    /**
     * Confirm asset verification with a code
     */
    suspend fun confirmAssetVerification(assetId: String, code: String): Boolean

    // ==================== Scanning ====================

    /**
     * Trigger a manual scan for all monitored assets
     */
    suspend fun triggerManualScan(): DarkWebMonitoringReport

    /**
     * Scan a specific asset
     */
    suspend fun scanAsset(assetId: String): List<DarkWebExposure>

    /**
     * Check if a specific value is exposed (without adding to monitoring)
     */
    suspend fun quickCheck(type: AssetType, value: String): List<DarkWebExposure>

    // ==================== Exposures ====================

    /**
     * Get all exposures
     */
    suspend fun getAllExposures(): List<DarkWebExposure>

    /**
     * Get exposures for a specific asset
     */
    suspend fun getExposuresForAsset(assetId: String): List<DarkWebExposure>

    /**
     * Observe exposures as a flow
     */
    fun observeExposures(): Flow<List<DarkWebExposure>>

    /**
     * Acknowledge an exposure
     */
    suspend fun acknowledgeExposure(exposureId: String)

    /**
     * Update remediation status for an exposure
     */
    suspend fun updateRemediationStatus(exposureId: String, status: RemediationStatus)

    /**
     * Get exposure details by ID
     */
    suspend fun getExposureDetails(exposureId: String): DarkWebExposure?

    // ==================== Reports ====================

    /**
     * Generate a comprehensive monitoring report
     */
    suspend fun generateReport(): DarkWebMonitoringReport

    /**
     * Get credential intelligence for an email
     */
    suspend fun getCredentialIntelligence(email: String): CredentialIntelligence

    // ==================== Alerts ====================

    /**
     * Get all alerts
     */
    suspend fun getAlerts(): List<DarkWebAlert>

    /**
     * Observe alerts as a flow
     */
    fun observeAlerts(): Flow<List<DarkWebAlert>>

    /**
     * Get unread alerts count
     */
    suspend fun getUnreadAlertsCount(): Int

    /**
     * Mark alert as read
     */
    suspend fun markAlertAsRead(alertId: String)

    /**
     * Mark all alerts as read
     */
    suspend fun markAllAlertsAsRead()

    /**
     * Dismiss an alert
     */
    suspend fun dismissAlert(alertId: String)

    // ==================== Identity Protection ====================

    /**
     * Get identity protection status
     */
    suspend fun getIdentityProtectionStatus(): IdentityProtectionStatus
}

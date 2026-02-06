package com.v7lthronyx.scamynx.domain.repository

import com.v7lthronyx.scamynx.domain.model.AlertPreferences
import com.v7lthronyx.scamynx.domain.model.DarkWebAlert
import com.v7lthronyx.scamynx.domain.model.DarkWebExposure
import com.v7lthronyx.scamynx.domain.model.DarkWebMonitoringConfig
import com.v7lthronyx.scamynx.domain.model.MonitoredAsset
import com.v7lthronyx.scamynx.domain.model.RemediationStatus
import com.v7lthronyx.scamynx.domain.model.ScanFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository for persisting dark web monitoring data.
 */
interface DarkWebMonitoringRepository {

    // ==================== Configuration ====================

    /**
     * Get monitoring configuration
     */
    suspend fun getConfig(): DarkWebMonitoringConfig

    /**
     * Save monitoring configuration
     */
    suspend fun saveConfig(config: DarkWebMonitoringConfig)

    /**
     * Update monitoring enabled state
     */
    suspend fun setMonitoringEnabled(enabled: Boolean)

    /**
     * Update scan frequency
     */
    suspend fun setScanFrequency(frequency: ScanFrequency)

    /**
     * Update alert preferences
     */
    suspend fun setAlertPreferences(preferences: AlertPreferences)

    /**
     * Update last scan timestamp
     */
    suspend fun updateLastScan(timestamp: Instant)

    /**
     * Update next scan timestamp
     */
    suspend fun updateNextScan(timestamp: Instant)

    // ==================== Monitored Assets ====================

    /**
     * Insert a new monitored asset
     */
    suspend fun insertAsset(asset: MonitoredAsset)

    /**
     * Update an existing asset
     */
    suspend fun updateAsset(asset: MonitoredAsset)

    /**
     * Delete a monitored asset
     */
    suspend fun deleteAsset(assetId: String)

    /**
     * Get asset by ID
     */
    suspend fun getAsset(assetId: String): MonitoredAsset?

    /**
     * Get all monitored assets
     */
    suspend fun getAllAssets(): List<MonitoredAsset>

    /**
     * Observe all monitored assets
     */
    fun observeAssets(): Flow<List<MonitoredAsset>>

    /**
     * Update asset exposure count
     */
    suspend fun updateAssetExposureCount(assetId: String, count: Int)

    /**
     * Update asset last checked timestamp
     */
    suspend fun updateAssetLastChecked(assetId: String, timestamp: Instant)

    // ==================== Exposures ====================

    /**
     * Insert exposures (batch)
     */
    suspend fun insertExposures(exposures: List<DarkWebExposure>)

    /**
     * Update an exposure
     */
    suspend fun updateExposure(exposure: DarkWebExposure)

    /**
     * Get exposure by ID
     */
    suspend fun getExposure(exposureId: String): DarkWebExposure?

    /**
     * Get all exposures
     */
    suspend fun getAllExposures(): List<DarkWebExposure>

    /**
     * Get exposures for a specific asset
     */
    suspend fun getExposuresForAsset(assetId: String): List<DarkWebExposure>

    /**
     * Observe all exposures
     */
    fun observeExposures(): Flow<List<DarkWebExposure>>

    /**
     * Get active exposures count
     */
    suspend fun getActiveExposuresCount(): Int

    /**
     * Mark exposure as acknowledged
     */
    suspend fun acknowledgeExposure(exposureId: String, timestamp: Instant)

    /**
     * Update remediation status
     */
    suspend fun updateRemediationStatus(exposureId: String, status: RemediationStatus)

    /**
     * Delete old exposures (cleanup)
     */
    suspend fun deleteOldExposures(before: Instant)

    // ==================== Alerts ====================

    /**
     * Insert an alert
     */
    suspend fun insertAlert(alert: DarkWebAlert)

    /**
     * Get all alerts
     */
    suspend fun getAllAlerts(): List<DarkWebAlert>

    /**
     * Observe alerts
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
     * Delete an alert
     */
    suspend fun deleteAlert(alertId: String)

    /**
     * Delete expired alerts
     */
    suspend fun deleteExpiredAlerts(before: Instant)
}

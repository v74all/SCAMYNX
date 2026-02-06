package com.v7lthronyx.scamynx.data.darkwebmonitoring

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DarkWebMonitoringDao {

    // ==================== Monitored Assets ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: MonitoredAssetEntity)

    @Update
    suspend fun updateAsset(asset: MonitoredAssetEntity)

    @Query("DELETE FROM monitored_assets WHERE id = :assetId")
    suspend fun deleteAsset(assetId: String)

    @Query("SELECT * FROM monitored_assets WHERE id = :assetId")
    suspend fun getAsset(assetId: String): MonitoredAssetEntity?

    @Query("SELECT * FROM monitored_assets ORDER BY addedAt DESC")
    suspend fun getAllAssets(): List<MonitoredAssetEntity>

    @Query("SELECT * FROM monitored_assets ORDER BY addedAt DESC")
    fun observeAssets(): Flow<List<MonitoredAssetEntity>>

    @Query("UPDATE monitored_assets SET exposureCount = :count WHERE id = :assetId")
    suspend fun updateAssetExposureCount(assetId: String, count: Int)

    @Query("UPDATE monitored_assets SET lastChecked = :timestamp WHERE id = :assetId")
    suspend fun updateAssetLastChecked(assetId: String, timestamp: Long)

    // ==================== Exposures ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExposures(exposures: List<DarkWebExposureEntity>)

    @Update
    suspend fun updateExposure(exposure: DarkWebExposureEntity)

    @Query("SELECT * FROM dark_web_exposures WHERE id = :exposureId")
    suspend fun getExposure(exposureId: String): DarkWebExposureEntity?

    @Query("SELECT * FROM dark_web_exposures ORDER BY discoveredAt DESC")
    suspend fun getAllExposures(): List<DarkWebExposureEntity>

    @Query("SELECT * FROM dark_web_exposures WHERE assetId = :assetId ORDER BY discoveredAt DESC")
    suspend fun getExposuresForAsset(assetId: String): List<DarkWebExposureEntity>

    @Query("SELECT * FROM dark_web_exposures ORDER BY discoveredAt DESC")
    fun observeExposures(): Flow<List<DarkWebExposureEntity>>

    @Query("SELECT COUNT(*) FROM dark_web_exposures WHERE isActive = 1")
    suspend fun getActiveExposuresCount(): Int

    @Query("UPDATE dark_web_exposures SET isAcknowledged = 1, acknowledgedAt = :timestamp WHERE id = :exposureId")
    suspend fun acknowledgeExposure(exposureId: String, timestamp: Long)

    @Query("UPDATE dark_web_exposures SET remediationStatus = :status WHERE id = :exposureId")
    suspend fun updateRemediationStatus(exposureId: String, status: String)

    @Query("DELETE FROM dark_web_exposures WHERE discoveredAt < :before")
    suspend fun deleteOldExposures(before: Long)

    // ==================== Alerts ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: DarkWebAlertEntity)

    @Query("SELECT * FROM dark_web_alerts ORDER BY createdAt DESC")
    suspend fun getAllAlerts(): List<DarkWebAlertEntity>

    @Query("SELECT * FROM dark_web_alerts ORDER BY createdAt DESC")
    fun observeAlerts(): Flow<List<DarkWebAlertEntity>>

    @Query("SELECT COUNT(*) FROM dark_web_alerts WHERE isRead = 0")
    suspend fun getUnreadAlertsCount(): Int

    @Query("UPDATE dark_web_alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAlertAsRead(alertId: String)

    @Query("UPDATE dark_web_alerts SET isRead = 1")
    suspend fun markAllAlertsAsRead()

    @Query("DELETE FROM dark_web_alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: String)

    @Query("DELETE FROM dark_web_alerts WHERE expiresAt IS NOT NULL AND expiresAt < :before")
    suspend fun deleteExpiredAlerts(before: Long)
}

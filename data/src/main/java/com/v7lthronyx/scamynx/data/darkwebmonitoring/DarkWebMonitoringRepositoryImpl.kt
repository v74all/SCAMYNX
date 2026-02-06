package com.v7lthronyx.scamynx.data.darkwebmonitoring

import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.AlertAction
import com.v7lthronyx.scamynx.domain.model.AlertPreferences
import com.v7lthronyx.scamynx.domain.model.AlertType
import com.v7lthronyx.scamynx.domain.model.DarkWebAlert
import com.v7lthronyx.scamynx.domain.model.DarkWebExposure
import com.v7lthronyx.scamynx.domain.model.DarkWebMonitoringConfig
import com.v7lthronyx.scamynx.domain.model.ExposedDataType
import com.v7lthronyx.scamynx.domain.model.ExposureContext
import com.v7lthronyx.scamynx.domain.model.MonitoredAsset
import com.v7lthronyx.scamynx.domain.model.RemediationStatus
import com.v7lthronyx.scamynx.domain.model.ScanFrequency
import com.v7lthronyx.scamynx.domain.repository.DarkWebMonitoringRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DarkWebMonitoringRepositoryImpl @Inject constructor(
    private val dao: DarkWebMonitoringDao,
    private val preferences: DarkWebMonitoringPreferences,
    private val json: Json,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : DarkWebMonitoringRepository {

    // ==================== Configuration ====================

    override suspend fun getConfig(): DarkWebMonitoringConfig = withContext(dispatcher) {
        val assets = dao.getAllAssets().map { it.toDomain() }
        DarkWebMonitoringConfig(
            isEnabled = preferences.isEnabled(),
            monitoredAssets = assets,
            alertPreferences = preferences.getAlertPreferences(),
            scanFrequency = preferences.getScanFrequency(),
            lastScan = preferences.getLastScan()?.let { Instant.fromEpochMilliseconds(it) },
            nextScan = preferences.getNextScan()?.let { Instant.fromEpochMilliseconds(it) },
            isPremium = preferences.isPremium(),
        )
    }

    override suspend fun saveConfig(config: DarkWebMonitoringConfig): Unit = withContext(dispatcher) {
        preferences.setEnabled(config.isEnabled)
        preferences.setPremium(config.isPremium)
        preferences.setScanFrequency(config.scanFrequency)
        preferences.setAlertPreferences(config.alertPreferences)
        config.lastScan?.let { preferences.setLastScan(it.toEpochMilliseconds()) }
        config.nextScan?.let { preferences.setNextScan(it.toEpochMilliseconds()) }
    }

    override suspend fun setMonitoringEnabled(enabled: Boolean) = withContext(dispatcher) {
        preferences.setEnabled(enabled)
    }

    override suspend fun setScanFrequency(frequency: ScanFrequency) = withContext(dispatcher) {
        preferences.setScanFrequency(frequency)
    }

    override suspend fun setAlertPreferences(preferences: AlertPreferences) = withContext(dispatcher) {
        this@DarkWebMonitoringRepositoryImpl.preferences.setAlertPreferences(preferences)
    }

    override suspend fun updateLastScan(timestamp: Instant) = withContext(dispatcher) {
        preferences.setLastScan(timestamp.toEpochMilliseconds())
    }

    override suspend fun updateNextScan(timestamp: Instant) = withContext(dispatcher) {
        preferences.setNextScan(timestamp.toEpochMilliseconds())
    }

    // ==================== Monitored Assets ====================

    override suspend fun insertAsset(asset: MonitoredAsset) = withContext(dispatcher) {
        dao.insertAsset(asset.toEntity())
    }

    override suspend fun updateAsset(asset: MonitoredAsset) = withContext(dispatcher) {
        dao.updateAsset(asset.toEntity())
    }

    override suspend fun deleteAsset(assetId: String) = withContext(dispatcher) {
        dao.deleteAsset(assetId)
    }

    override suspend fun getAsset(assetId: String): MonitoredAsset? = withContext(dispatcher) {
        dao.getAsset(assetId)?.toDomain()
    }

    override suspend fun getAllAssets(): List<MonitoredAsset> = withContext(dispatcher) {
        dao.getAllAssets().map { it.toDomain() }
    }

    override fun observeAssets(): Flow<List<MonitoredAsset>> {
        return dao.observeAssets().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun updateAssetExposureCount(assetId: String, count: Int) = withContext(dispatcher) {
        dao.updateAssetExposureCount(assetId, count)
    }

    override suspend fun updateAssetLastChecked(assetId: String, timestamp: Instant) = withContext(dispatcher) {
        dao.updateAssetLastChecked(assetId, timestamp.toEpochMilliseconds())
    }

    // ==================== Exposures ====================

    override suspend fun insertExposures(exposures: List<DarkWebExposure>) = withContext(dispatcher) {
        dao.insertExposures(exposures.map { it.toEntity() })
    }

    override suspend fun updateExposure(exposure: DarkWebExposure) = withContext(dispatcher) {
        dao.updateExposure(exposure.toEntity())
    }

    override suspend fun getExposure(exposureId: String): DarkWebExposure? = withContext(dispatcher) {
        dao.getExposure(exposureId)?.toDomain()
    }

    override suspend fun getAllExposures(): List<DarkWebExposure> = withContext(dispatcher) {
        dao.getAllExposures().map { it.toDomain() }
    }

    override suspend fun getExposuresForAsset(assetId: String): List<DarkWebExposure> = withContext(dispatcher) {
        dao.getExposuresForAsset(assetId).map { it.toDomain() }
    }

    override fun observeExposures(): Flow<List<DarkWebExposure>> {
        return dao.observeExposures().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getActiveExposuresCount(): Int = withContext(dispatcher) {
        dao.getActiveExposuresCount()
    }

    override suspend fun acknowledgeExposure(exposureId: String, timestamp: Instant) = withContext(dispatcher) {
        dao.acknowledgeExposure(exposureId, timestamp.toEpochMilliseconds())
    }

    override suspend fun updateRemediationStatus(exposureId: String, status: RemediationStatus) = withContext(dispatcher) {
        dao.updateRemediationStatus(exposureId, status.name)
    }

    override suspend fun deleteOldExposures(before: Instant) = withContext(dispatcher) {
        dao.deleteOldExposures(before.toEpochMilliseconds())
    }

    // ==================== Alerts ====================

    override suspend fun insertAlert(alert: DarkWebAlert) = withContext(dispatcher) {
        dao.insertAlert(alert.toEntity())
    }

    override suspend fun getAllAlerts(): List<DarkWebAlert> = withContext(dispatcher) {
        dao.getAllAlerts().map { it.toDomain() }
    }

    override fun observeAlerts(): Flow<List<DarkWebAlert>> {
        return dao.observeAlerts().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getUnreadAlertsCount(): Int = withContext(dispatcher) {
        dao.getUnreadAlertsCount()
    }

    override suspend fun markAlertAsRead(alertId: String) = withContext(dispatcher) {
        dao.markAlertAsRead(alertId)
    }

    override suspend fun markAllAlertsAsRead() = withContext(dispatcher) {
        dao.markAllAlertsAsRead()
    }

    override suspend fun deleteAlert(alertId: String) = withContext(dispatcher) {
        dao.deleteAlert(alertId)
    }

    override suspend fun deleteExpiredAlerts(before: Instant) = withContext(dispatcher) {
        dao.deleteExpiredAlerts(before.toEpochMilliseconds())
    }

    // ==================== Mappers ====================

    private fun MonitoredAsset.toEntity(): MonitoredAssetEntity {
        return MonitoredAssetEntity(
            id = id,
            type = type,
            value = value,
            maskedValue = maskedValue,
            isVerified = isVerified,
            addedAt = addedAt.toEpochMilliseconds(),
            lastChecked = lastChecked?.toEpochMilliseconds(),
            exposureCount = exposureCount,
            status = status,
        )
    }

    private fun MonitoredAssetEntity.toDomain(): MonitoredAsset {
        return MonitoredAsset(
            id = id,
            type = type,
            value = value,
            maskedValue = maskedValue,
            isVerified = isVerified,
            addedAt = Instant.fromEpochMilliseconds(addedAt),
            lastChecked = lastChecked?.let { Instant.fromEpochMilliseconds(it) },
            exposureCount = exposureCount,
            status = status,
        )
    }

    private fun DarkWebExposure.toEntity(): DarkWebExposureEntity {
        return DarkWebExposureEntity(
            id = id,
            assetId = assetId,
            assetType = assetType,
            maskedAsset = maskedAsset,
            source = source,
            marketplace = marketplace,
            forumName = forumName,
            pastebin = pastebin,
            exposureType = exposureType,
            exposedDataJson = json.encodeToString(exposedData.map { it.name }),
            severity = severity,
            confidence = confidence,
            contextJson = context?.let { json.encodeToString(it) },
            relatedExposuresJson = json.encodeToString(relatedExposures),
            discoveredAt = discoveredAt.toEpochMilliseconds(),
            estimatedExposureDate = estimatedExposureDate?.toEpochMilliseconds(),
            lastSeenAt = lastSeenAt?.toEpochMilliseconds(),
            isActive = isActive,
            isAcknowledged = isAcknowledged,
            acknowledgedAt = acknowledgedAt?.toEpochMilliseconds(),
            remediationStatus = remediationStatus,
            recommendationsJson = json.encodeToString(recommendations),
        )
    }

    private fun DarkWebExposureEntity.toDomain(): DarkWebExposure {
        return DarkWebExposure(
            id = id,
            assetId = assetId,
            assetType = assetType,
            maskedAsset = maskedAsset,
            source = source,
            marketplace = marketplace,
            forumName = forumName,
            pastebin = pastebin,
            exposureType = exposureType,
            exposedData = try {
                json.decodeFromString<List<String>>(exposedDataJson).mapNotNull { 
                    try { ExposedDataType.valueOf(it) } catch (e: Exception) { null }
                }
            } catch (e: Exception) { emptyList() },
            severity = severity,
            confidence = confidence,
            context = contextJson?.let { 
                try { json.decodeFromString<ExposureContext>(it) } catch (e: Exception) { null }
            },
            relatedExposures = try {
                json.decodeFromString<List<String>>(relatedExposuresJson)
            } catch (e: Exception) { emptyList() },
            discoveredAt = Instant.fromEpochMilliseconds(discoveredAt),
            estimatedExposureDate = estimatedExposureDate?.let { Instant.fromEpochMilliseconds(it) },
            lastSeenAt = lastSeenAt?.let { Instant.fromEpochMilliseconds(it) },
            isActive = isActive,
            isAcknowledged = isAcknowledged,
            acknowledgedAt = acknowledgedAt?.let { Instant.fromEpochMilliseconds(it) },
            remediationStatus = remediationStatus,
            recommendations = try {
                json.decodeFromString<List<String>>(recommendationsJson)
            } catch (e: Exception) { emptyList() },
        )
    }

    private fun DarkWebAlert.toEntity(): DarkWebAlertEntity {
        return DarkWebAlertEntity(
            id = id,
            type = type.name,
            severity = severity,
            title = title,
            message = message,
            exposureId = exposureId,
            assetId = assetId,
            isRead = isRead,
            isActioned = isActioned,
            createdAt = createdAt.toEpochMilliseconds(),
            expiresAt = expiresAt?.toEpochMilliseconds(),
            actionsJson = json.encodeToString(actions),
        )
    }

    private fun DarkWebAlertEntity.toDomain(): DarkWebAlert {
        return DarkWebAlert(
            id = id,
            type = try { AlertType.valueOf(type) } catch (e: Exception) { AlertType.NEW_EXPOSURE },
            severity = severity,
            title = title,
            message = message,
            exposureId = exposureId,
            assetId = assetId,
            isRead = isRead,
            isActioned = isActioned,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            expiresAt = expiresAt?.let { Instant.fromEpochMilliseconds(it) },
            actions = try {
                json.decodeFromString<List<AlertAction>>(actionsJson)
            } catch (e: Exception) { emptyList() },
        )
    }
}

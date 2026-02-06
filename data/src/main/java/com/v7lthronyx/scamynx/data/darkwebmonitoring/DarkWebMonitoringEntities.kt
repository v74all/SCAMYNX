package com.v7lthronyx.scamynx.data.darkwebmonitoring

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.v7lthronyx.scamynx.domain.model.AssetType
import com.v7lthronyx.scamynx.domain.model.BreachSeverity
import com.v7lthronyx.scamynx.domain.model.ConfidenceLevel
import com.v7lthronyx.scamynx.domain.model.ExposureSource
import com.v7lthronyx.scamynx.domain.model.ExposureType
import com.v7lthronyx.scamynx.domain.model.MonitoringStatus
import com.v7lthronyx.scamynx.domain.model.RemediationStatus

/**
 * Room entity for monitored assets
 */
@Entity(tableName = "monitored_assets")
data class MonitoredAssetEntity(
    @PrimaryKey
    val id: String,
    val type: AssetType,
    val value: String,
    val maskedValue: String,
    val isVerified: Boolean,
    val addedAt: Long, // epoch millis
    val lastChecked: Long?, // epoch millis
    val exposureCount: Int,
    val status: MonitoringStatus,
)

/**
 * Room entity for dark web exposures
 */
@Entity(tableName = "dark_web_exposures")
data class DarkWebExposureEntity(
    @PrimaryKey
    val id: String,
    val assetId: String,
    val assetType: AssetType,
    val maskedAsset: String,
    val source: ExposureSource,
    val marketplace: String?,
    val forumName: String?,
    val pastebin: String?,
    val exposureType: ExposureType,
    val exposedDataJson: String, // JSON list of ExposedDataType
    val severity: BreachSeverity,
    val confidence: ConfidenceLevel,
    val contextJson: String?, // JSON ExposureContext
    val relatedExposuresJson: String, // JSON list of strings
    val discoveredAt: Long, // epoch millis
    val estimatedExposureDate: Long?, // epoch millis
    val lastSeenAt: Long?, // epoch millis
    val isActive: Boolean,
    val isAcknowledged: Boolean,
    val acknowledgedAt: Long?, // epoch millis
    val remediationStatus: RemediationStatus,
    val recommendationsJson: String, // JSON list of strings
)

/**
 * Room entity for dark web alerts
 */
@Entity(tableName = "dark_web_alerts")
data class DarkWebAlertEntity(
    @PrimaryKey
    val id: String,
    val type: String, // AlertType as string
    val severity: BreachSeverity,
    val title: String,
    val message: String,
    val exposureId: String?,
    val assetId: String?,
    val isRead: Boolean,
    val isActioned: Boolean,
    val createdAt: Long, // epoch millis
    val expiresAt: Long?, // epoch millis
    val actionsJson: String, // JSON list of AlertAction
)

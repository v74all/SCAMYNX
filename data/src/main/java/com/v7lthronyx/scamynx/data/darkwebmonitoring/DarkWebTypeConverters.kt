package com.v7lthronyx.scamynx.data.darkwebmonitoring

import androidx.room.TypeConverter
import com.v7lthronyx.scamynx.domain.model.AssetType
import com.v7lthronyx.scamynx.domain.model.BreachSeverity
import com.v7lthronyx.scamynx.domain.model.ConfidenceLevel
import com.v7lthronyx.scamynx.domain.model.ExposureSource
import com.v7lthronyx.scamynx.domain.model.ExposureType
import com.v7lthronyx.scamynx.domain.model.MonitoringStatus
import com.v7lthronyx.scamynx.domain.model.RemediationStatus

/**
 * Room TypeConverters for Dark Web Monitoring entities.
 */
class DarkWebTypeConverters {

    // AssetType
    @TypeConverter
    fun fromAssetType(value: AssetType): String = value.name

    @TypeConverter
    fun toAssetType(value: String): AssetType = 
        try { AssetType.valueOf(value) } catch (e: Exception) { AssetType.EMAIL }

    // MonitoringStatus
    @TypeConverter
    fun fromMonitoringStatus(value: MonitoringStatus): String = value.name

    @TypeConverter
    fun toMonitoringStatus(value: String): MonitoringStatus = 
        try { MonitoringStatus.valueOf(value) } catch (e: Exception) { MonitoringStatus.PENDING_VERIFICATION }

    // ExposureSource
    @TypeConverter
    fun fromExposureSource(value: ExposureSource): String = value.name

    @TypeConverter
    fun toExposureSource(value: String): ExposureSource = 
        try { ExposureSource.valueOf(value) } catch (e: Exception) { ExposureSource.UNKNOWN }

    // ExposureType
    @TypeConverter
    fun fromExposureType(value: ExposureType): String = value.name

    @TypeConverter
    fun toExposureType(value: String): ExposureType = 
        try { ExposureType.valueOf(value) } catch (e: Exception) { ExposureType.CREDENTIAL_LEAK }

    // BreachSeverity
    @TypeConverter
    fun fromBreachSeverity(value: BreachSeverity): String = value.name

    @TypeConverter
    fun toBreachSeverity(value: String): BreachSeverity = 
        try { BreachSeverity.valueOf(value) } catch (e: Exception) { BreachSeverity.LOW }

    // ConfidenceLevel
    @TypeConverter
    fun fromConfidenceLevel(value: ConfidenceLevel): String = value.name

    @TypeConverter
    fun toConfidenceLevel(value: String): ConfidenceLevel = 
        try { ConfidenceLevel.valueOf(value) } catch (e: Exception) { ConfidenceLevel.LOW }

    // RemediationStatus
    @TypeConverter
    fun fromRemediationStatus(value: RemediationStatus): String = value.name

    @TypeConverter
    fun toRemediationStatus(value: String): RemediationStatus = 
        try { RemediationStatus.valueOf(value) } catch (e: Exception) { RemediationStatus.PENDING }
}

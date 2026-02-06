package com.v7lthronyx.scamynx.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.v7lthronyx.scamynx.data.darkwebmonitoring.DarkWebAlertEntity
import com.v7lthronyx.scamynx.data.darkwebmonitoring.DarkWebExposureEntity
import com.v7lthronyx.scamynx.data.darkwebmonitoring.DarkWebMonitoringDao
import com.v7lthronyx.scamynx.data.darkwebmonitoring.DarkWebTypeConverters
import com.v7lthronyx.scamynx.data.darkwebmonitoring.MonitoredAssetEntity
import com.v7lthronyx.scamynx.data.qrcode.QRCodeHistoryRoomDao
import com.v7lthronyx.scamynx.data.realtimeprotection.BlockedThreatsRoomDao

@Database(
    entities = [
        ScanEntity::class,
        VendorVerdictEntity::class,
        ThreatIndicatorEntity::class,
        MonitoredAssetEntity::class,
        DarkWebExposureEntity::class,
        DarkWebAlertEntity::class,
        PrivacyEventEntity::class,
        PrivacyBaselineEntity::class,
        QRCodeScanEntity::class,
        BlockedThreatEntity::class,
        TelemetryEventEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
@TypeConverters(DarkWebTypeConverters::class)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun threatFeedDao(): ThreatFeedDao
    abstract fun darkWebMonitoringDao(): DarkWebMonitoringDao
    abstract fun privacyEventDao(): PrivacyEventDao
    abstract fun privacyBaselineDao(): PrivacyBaselineDao
    abstract fun qrCodeHistoryDao(): QRCodeHistoryRoomDao
    abstract fun blockedThreatsDao(): BlockedThreatsRoomDao
    abstract fun telemetryEventDao(): TelemetryEventDao
}

package com.v7lthronyx.scamynx.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ScanDatabaseMigrations {

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `privacy_events` (
                    `event_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `package_name` TEXT NOT NULL,
                    `source_id` TEXT NOT NULL,
                    `event_type` TEXT NOT NULL,
                    `resource_type` TEXT NOT NULL,
                    `timestamp_epoch` INTEGER NOT NULL,
                    `duration_ms` INTEGER,
                    `visibility_context` TEXT NOT NULL,
                    `session_id` TEXT,
                    `screen_name` TEXT,
                    `activity_class` TEXT,
                    `confidence` TEXT NOT NULL,
                    `priority` TEXT NOT NULL,
                    `metadata_json` TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_privacy_events_package_name` ON `privacy_events` (`package_name`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_privacy_events_resource_type` ON `privacy_events` (`resource_type`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_privacy_events_timestamp_epoch` ON `privacy_events` (`timestamp_epoch`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `privacy_baselines` (
                    `package_name` TEXT NOT NULL,
                    `resource_type` TEXT NOT NULL,
                    `average_daily_count` REAL NOT NULL,
                    `std_daily_count` REAL NOT NULL,
                    `median_duration_ms` INTEGER,
                    `last_seen_epoch` INTEGER,
                    `last_seen_foreground_state` TEXT,
                    `expected_visibility` TEXT,
                    `permission_mismatch_score` REAL NOT NULL,
                    `override_conflict_score` REAL NOT NULL,
                    `updated_at_epoch` INTEGER NOT NULL,
                    PRIMARY KEY(`package_name`, `resource_type`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create monitored_assets table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `monitored_assets` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `type` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    `maskedValue` TEXT NOT NULL,
                    `isVerified` INTEGER NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    `lastChecked` INTEGER,
                    `exposureCount` INTEGER NOT NULL,
                    `status` TEXT NOT NULL
                )
                """.trimIndent(),
            )

            // Create dark_web_exposures table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `dark_web_exposures` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `assetId` TEXT NOT NULL,
                    `assetType` TEXT NOT NULL,
                    `maskedAsset` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `marketplace` TEXT,
                    `forumName` TEXT,
                    `pastebin` TEXT,
                    `exposureType` TEXT NOT NULL,
                    `exposedDataJson` TEXT NOT NULL,
                    `severity` TEXT NOT NULL,
                    `confidence` TEXT NOT NULL,
                    `contextJson` TEXT,
                    `relatedExposuresJson` TEXT NOT NULL,
                    `discoveredAt` INTEGER NOT NULL,
                    `estimatedExposureDate` INTEGER,
                    `lastSeenAt` INTEGER,
                    `isActive` INTEGER NOT NULL,
                    `isAcknowledged` INTEGER NOT NULL,
                    `acknowledgedAt` INTEGER,
                    `remediationStatus` TEXT NOT NULL,
                    `recommendationsJson` TEXT NOT NULL
                )
                """.trimIndent(),
            )

            // Create dark_web_alerts table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `dark_web_alerts` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `type` TEXT NOT NULL,
                    `severity` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `message` TEXT NOT NULL,
                    `exposureId` TEXT,
                    `assetId` TEXT,
                    `isRead` INTEGER NOT NULL,
                    `isActioned` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `expiresAt` INTEGER,
                    `actionsJson` TEXT NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `privacy_events` (
                    `event_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `package_name` TEXT NOT NULL,
                    `source_id` TEXT NOT NULL,
                    `event_type` TEXT NOT NULL,
                    `resource_type` TEXT NOT NULL,
                    `timestamp_epoch` INTEGER NOT NULL,
                    `duration_ms` INTEGER,
                    `visibility_context` TEXT NOT NULL,
                    `session_id` TEXT,
                    `screen_name` TEXT,
                    `activity_class` TEXT,
                    `confidence` TEXT NOT NULL,
                    `priority` TEXT NOT NULL,
                    `metadata_json` TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_privacy_events_package_name` ON `privacy_events` (`package_name`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_privacy_events_resource_type` ON `privacy_events` (`resource_type`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_privacy_events_timestamp_epoch` ON `privacy_events` (`timestamp_epoch`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `privacy_baselines` (
                    `package_name` TEXT NOT NULL,
                    `resource_type` TEXT NOT NULL,
                    `average_daily_count` REAL NOT NULL,
                    `std_daily_count` REAL NOT NULL,
                    `median_duration_ms` INTEGER,
                    `last_seen_epoch` INTEGER,
                    `last_seen_foreground_state` TEXT,
                    `expected_visibility` TEXT,
                    `permission_mismatch_score` REAL NOT NULL,
                    `override_conflict_score` REAL NOT NULL,
                    `updated_at_epoch` INTEGER NOT NULL,
                    PRIMARY KEY(`package_name`, `resource_type`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `qr_scans` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `raw_content` TEXT NOT NULL,
                    `content_type` TEXT NOT NULL,
                    `parsed_content_json` TEXT NOT NULL,
                    `is_safe` INTEGER NOT NULL,
                    `threat_assessment_json` TEXT,
                    `metadata_json` TEXT NOT NULL,
                    `scanned_at_epoch` INTEGER NOT NULL,
                    `source` TEXT NOT NULL,
                    `was_blocked` INTEGER NOT NULL,
                    `reported_reason` TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_qr_scans_scanned_at_epoch` ON `qr_scans` (`scanned_at_epoch`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_qr_scans_content_type` ON `qr_scans` (`content_type`)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `blocked_threats` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `threat_type` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `target` TEXT NOT NULL,
                    `severity` TEXT NOT NULL,
                    `action` TEXT NOT NULL,
                    `reason` TEXT NOT NULL,
                    `blocked_at_epoch` INTEGER NOT NULL,
                    `source_app` TEXT,
                    `user_notified` INTEGER NOT NULL,
                    `can_unblock` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_blocked_threats_blocked_at_epoch` ON `blocked_threats` (`blocked_at_epoch`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_blocked_threats_source` ON `blocked_threats` (`source`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_blocked_threats_threat_type` ON `blocked_threats` (`threat_type`)",
            )
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `telemetry_events` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `event_json` TEXT NOT NULL,
                    `created_at_epoch` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_telemetry_events_created_at` ON `telemetry_events` (`created_at_epoch`)",
            )
        }
    }
}

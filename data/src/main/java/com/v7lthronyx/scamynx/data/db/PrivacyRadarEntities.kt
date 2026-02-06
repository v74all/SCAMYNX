package com.v7lthronyx.scamynx.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "privacy_events",
    indices = [
        Index(value = ["package_name"]),
        Index(value = ["resource_type"]),
        Index(value = ["timestamp_epoch"]),
    ],
)
data class PrivacyEventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "event_id")
    val eventId: Long = 0,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "source_id")
    val sourceId: String,
    @ColumnInfo(name = "event_type")
    val eventType: String,
    @ColumnInfo(name = "resource_type")
    val resourceType: String,
    @ColumnInfo(name = "timestamp_epoch")
    val timestampEpochMillis: Long,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long?,
    @ColumnInfo(name = "visibility_context")
    val visibilityContext: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String?,
    @ColumnInfo(name = "screen_name")
    val screenName: String?,
    @ColumnInfo(name = "activity_class")
    val activityClassName: String?,
    @ColumnInfo(name = "confidence")
    val confidence: String,
    @ColumnInfo(name = "priority")
    val priority: String,
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String?,
)

@Entity(
    tableName = "privacy_baselines",
    primaryKeys = ["package_name", "resource_type"],
)
data class PrivacyBaselineEntity(
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "resource_type")
    val resourceType: String,
    @ColumnInfo(name = "average_daily_count")
    val averageDailyCount: Double,
    @ColumnInfo(name = "std_daily_count")
    val standardDeviationDailyCount: Double,
    @ColumnInfo(name = "median_duration_ms")
    val medianDurationMs: Long?,
    @ColumnInfo(name = "last_seen_epoch")
    val lastSeenEpochMillis: Long?,
    @ColumnInfo(name = "last_seen_foreground_state")
    val lastSeenForegroundState: String?,
    @ColumnInfo(name = "expected_visibility")
    val expectedVisibility: String?,
    @ColumnInfo(name = "permission_mismatch_score")
    val permissionMismatchScore: Double,
    @ColumnInfo(name = "override_conflict_score")
    val overrideConflictScore: Double,
    @ColumnInfo(name = "updated_at_epoch")
    val updatedAtEpochMillis: Long,
)

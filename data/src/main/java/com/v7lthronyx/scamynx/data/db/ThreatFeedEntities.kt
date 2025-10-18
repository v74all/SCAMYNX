package com.v7lthronyx.scamynx.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threat_indicators")
data class ThreatIndicatorEntity(
    @PrimaryKey
    @ColumnInfo(name = "indicator_id")
    val indicatorId: String,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "risk_score")
    val riskScore: Double,
    @ColumnInfo(name = "tags_json")
    val tagsJson: String,
    @ColumnInfo(name = "last_seen_epoch")
    val lastSeenEpochMillis: Long?,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "fetched_at_epoch")
    val fetchedAtEpochMillis: Long,
)

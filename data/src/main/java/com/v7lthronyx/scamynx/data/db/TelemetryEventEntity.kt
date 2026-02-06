package com.v7lthronyx.scamynx.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_events")
data class TelemetryEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "event_json")
    val eventJson: String,
    @ColumnInfo(name = "created_at_epoch")
    val createdAtEpoch: Long,
)

package com.v7lthronyx.scamynx.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_threats",
    indices = [
        Index("blocked_at_epoch"),
        Index("source"),
        Index("threat_type"),
    ],
)
data class BlockedThreatEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "threat_type")
    val threatType: String,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "target")
    val target: String,
    @ColumnInfo(name = "severity")
    val severity: String,
    @ColumnInfo(name = "action")
    val action: String,
    @ColumnInfo(name = "reason")
    val reason: String,
    @ColumnInfo(name = "blocked_at_epoch")
    val blockedAtEpoch: Long,
    @ColumnInfo(name = "source_app")
    val sourceApp: String?,
    @ColumnInfo(name = "user_notified")
    val userNotified: Boolean,
    @ColumnInfo(name = "can_unblock")
    val canUnblock: Boolean,
)

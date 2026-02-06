package com.v7lthronyx.scamynx.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "qr_scans",
    indices = [
        Index("scanned_at_epoch"),
        Index("content_type"),
    ],
)
data class QRCodeScanEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "raw_content")
    val rawContent: String,
    @ColumnInfo(name = "content_type")
    val contentType: String,
    @ColumnInfo(name = "parsed_content_json")
    val parsedContentJson: String,
    @ColumnInfo(name = "is_safe")
    val isSafe: Boolean,
    @ColumnInfo(name = "threat_assessment_json")
    val threatAssessmentJson: String?,
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String,
    @ColumnInfo(name = "scanned_at_epoch")
    val scannedAtEpoch: Long,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "was_blocked")
    val wasBlocked: Boolean,
    @ColumnInfo(name = "reported_reason")
    val reportedReason: String? = null,
)

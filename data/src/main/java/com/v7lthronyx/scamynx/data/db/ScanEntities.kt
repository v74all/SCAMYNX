package com.v7lthronyx.scamynx.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey
    @ColumnInfo(name = "scan_id")
    val scanId: String,
    @ColumnInfo(name = "target_type")
    val targetType: String,
    @ColumnInfo(name = "target_label")
    val targetLabel: String,
    @ColumnInfo(name = "normalized_url")
    val normalizedUrl: String?,
    @ColumnInfo(name = "risk_score")
    val riskScore: Double,
    @ColumnInfo(name = "breakdown_json")
    val breakdownJson: String,
    @ColumnInfo(name = "network_json")
    val networkJson: String?,
    @ColumnInfo(name = "ml_json")
    val mlJson: String?,
    @ColumnInfo(name = "file_json")
    val fileJson: String?,
    @ColumnInfo(name = "vpn_json")
    val vpnJson: String?,
    @ColumnInfo(name = "instagram_json")
    val instagramJson: String?,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "vendor_verdicts",
    primaryKeys = ["scan_id", "provider"],
)
data class VendorVerdictEntity(
    @ColumnInfo(name = "scan_id")
    val scanId: String,
    @ColumnInfo(name = "provider")
    val provider: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "score")
    val score: Double,
    @ColumnInfo(name = "details_json")
    val detailsJson: String,
)

data class ScanWithVerdicts(
    @Embedded
    val scan: ScanEntity,
    @Relation(
        parentColumn = "scan_id",
        entityColumn = "scan_id",
    )
    val verdicts: List<VendorVerdictEntity>,
)

package com.v7lthronyx.scamynx.data.qrcode

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.v7lthronyx.scamynx.data.db.QRCodeScanEntity

@Dao
interface QRCodeHistoryRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(entity: QRCodeScanEntity)

    @Query("SELECT * FROM qr_scans WHERE id = :scanId LIMIT 1")
    suspend fun getScan(scanId: String): QRCodeScanEntity?

    @Query("SELECT * FROM qr_scans ORDER BY scanned_at_epoch DESC LIMIT :limit OFFSET :offset")
    suspend fun getHistory(limit: Int, offset: Int): List<QRCodeScanEntity>

    @Query(
        "SELECT * FROM qr_scans WHERE content_type IN (:contentTypes) " +
            "ORDER BY scanned_at_epoch DESC LIMIT :limit OFFSET :offset",
    )
    suspend fun getHistoryFiltered(
        limit: Int,
        offset: Int,
        contentTypes: List<String>,
    ): List<QRCodeScanEntity>

    @Query("DELETE FROM qr_scans WHERE id = :scanId")
    suspend fun deleteScan(scanId: String)

    @Query("DELETE FROM qr_scans")
    suspend fun clearAll()

    @Query("UPDATE qr_scans SET reported_reason = :reason WHERE id = :scanId")
    suspend fun markAsReported(scanId: String, reason: String)

    @Query("SELECT * FROM qr_scans WHERE scanned_at_epoch >= :startEpoch")
    suspend fun getScansSince(startEpoch: Long): List<QRCodeScanEntity>
}

package com.v7lthronyx.scamynx.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Transaction
    @Query("SELECT * FROM scans ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun observeHistory(limit: Int, offset: Int): Flow<List<ScanWithVerdicts>>

    @Transaction
    @Query("SELECT * FROM scans ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getHistory(limit: Int, offset: Int): List<ScanWithVerdicts>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scanEntity: ScanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerdicts(verdicts: List<VendorVerdictEntity>)

    @Query("DELETE FROM vendor_verdicts WHERE scan_id = :scanId")
    suspend fun deleteVerdictsForScan(scanId: String)

    @Transaction
    @Query("SELECT * FROM scans WHERE scan_id = :scanId LIMIT 1")
    suspend fun getScan(scanId: String): ScanWithVerdicts?

    @Query("DELETE FROM scans")
    suspend fun clearAllScans()

    @Query("DELETE FROM vendor_verdicts")
    suspend fun clearAllVerdicts()
}

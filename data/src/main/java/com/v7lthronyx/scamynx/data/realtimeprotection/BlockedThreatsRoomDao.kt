package com.v7lthronyx.scamynx.data.realtimeprotection

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.v7lthronyx.scamynx.data.db.BlockedThreatEntity

@Dao
interface BlockedThreatsRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreat(entity: BlockedThreatEntity)

    @Query("DELETE FROM blocked_threats WHERE id = :threatId")
    suspend fun deleteThreat(threatId: String)

    @Query(
        "SELECT * FROM blocked_threats ORDER BY blocked_at_epoch DESC " +
            "LIMIT :limit OFFSET :offset",
    )
    suspend fun getBlockedThreats(limit: Int, offset: Int): List<BlockedThreatEntity>

    @Query("DELETE FROM blocked_threats WHERE blocked_at_epoch < :cutoff")
    suspend fun clearOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM blocked_threats WHERE source = :source")
    suspend fun getCountBySource(source: String): Int

    @Query("SELECT COUNT(*) FROM blocked_threats WHERE blocked_at_epoch >= :cutoff")
    suspend fun getCountSince(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM blocked_threats")
    suspend fun getTotalCount(): Int

    @Query("SELECT blocked_at_epoch FROM blocked_threats ORDER BY blocked_at_epoch DESC LIMIT 1")
    suspend fun getLastBlockedEpoch(): Long?

    @Query("SELECT * FROM blocked_threats WHERE blocked_at_epoch >= :startEpoch")
    suspend fun getThreatsSince(startEpoch: Long): List<BlockedThreatEntity>
}

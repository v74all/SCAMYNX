package com.v7lthronyx.scamynx.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TelemetryEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TelemetryEventEntity)

    @Query(
        "SELECT * FROM telemetry_events " +
            "ORDER BY created_at_epoch ASC " +
            "LIMIT :limit",
    )
    suspend fun fetchOldest(limit: Int): List<TelemetryEventEntity>

    @Query("DELETE FROM telemetry_events WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM telemetry_events")
    suspend fun count(): Int

    @Query("DELETE FROM telemetry_events WHERE created_at_epoch < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

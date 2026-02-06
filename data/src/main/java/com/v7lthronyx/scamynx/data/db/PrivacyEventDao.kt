package com.v7lthronyx.scamynx.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivacyEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: PrivacyEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(events: List<PrivacyEventEntity>): List<Long>

    @Query(
        value = """
            SELECT * FROM privacy_events 
            ORDER BY timestamp_epoch DESC 
            LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getRecentEvents(limit: Int, offset: Int = 0): List<PrivacyEventEntity>

    @Query(
        value = """
            SELECT * FROM privacy_events
            WHERE package_name = :packageName
              AND resource_type = :resourceType
              AND timestamp_epoch BETWEEN :startEpochMillis AND :endEpochMillis
            ORDER BY timestamp_epoch DESC
        """,
    )
    suspend fun getEventsForPackage(
        packageName: String,
        resourceType: String,
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): List<PrivacyEventEntity>

    @Query(
        value = """
            SELECT * FROM privacy_events
            ORDER BY timestamp_epoch DESC
            LIMIT :limit
        """,
    )
    fun observeRecentEvents(limit: Int): Flow<List<PrivacyEventEntity>>

    @Query("DELETE FROM privacy_events WHERE timestamp_epoch < :thresholdEpochMillis")
    suspend fun purgeOlderThan(thresholdEpochMillis: Long): Int

    @Query(
        value = """
            SELECT package_name, resource_type 
            FROM privacy_events 
            GROUP BY package_name, resource_type
        """,
    )
    suspend fun getDistinctEventKeys(): List<PrivacyEventKey>
}

data class PrivacyEventKey(
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "resource_type")
    val resourceType: String,
)

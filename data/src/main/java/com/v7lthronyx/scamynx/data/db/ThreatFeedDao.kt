package com.v7lthronyx.scamynx.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatFeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(indicators: List<ThreatIndicatorEntity>)

    @Query("DELETE FROM threat_indicators")
    suspend fun clearAll()

    @Query("SELECT * FROM threat_indicators ORDER BY fetched_at_epoch DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<ThreatIndicatorEntity>>

    @Query("SELECT * FROM threat_indicators WHERE url = :url")
    suspend fun findByUrl(url: String): List<ThreatIndicatorEntity>

    @Query(
        "SELECT * FROM threat_indicators " +
            "WHERE url LIKE '%' || :host || '%' " +
            "ORDER BY fetched_at_epoch DESC " +
            "LIMIT :limit",
    )
    suspend fun findByHost(host: String, limit: Int): List<ThreatIndicatorEntity>
}

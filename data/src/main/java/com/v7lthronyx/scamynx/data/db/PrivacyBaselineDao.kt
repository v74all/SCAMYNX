package com.v7lthronyx.scamynx.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivacyBaselineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(baseline: PrivacyBaselineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(baselines: List<PrivacyBaselineEntity>)

    @Query(
        value = """
            SELECT * FROM privacy_baselines
            WHERE package_name = :packageName AND resource_type = :resourceType
            LIMIT 1
        """,
    )
    suspend fun getBaseline(
        packageName: String,
        resourceType: String,
    ): PrivacyBaselineEntity?

    @Query("DELETE FROM privacy_baselines WHERE package_name = :packageName AND resource_type = :resourceType")
    suspend fun deleteBaseline(packageName: String, resourceType: String): Int

    @Query("SELECT * FROM privacy_baselines WHERE resource_type = :resourceType")
    fun observeBaselinesForResource(resourceType: String): Flow<List<PrivacyBaselineEntity>>

    @Query("SELECT * FROM privacy_baselines")
    fun observeAllBaselines(): Flow<List<PrivacyBaselineEntity>>
}

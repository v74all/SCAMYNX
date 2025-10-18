package com.v7lthronyx.scamynx.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScanEntity::class,
        VendorVerdictEntity::class,
        ThreatIndicatorEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun threatFeedDao(): ThreatFeedDao
}

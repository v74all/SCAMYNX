package com.v7lthronyx.scamynx.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.v7lthronyx.scamynx.data.db.PrivacyBaselineDao
import com.v7lthronyx.scamynx.data.db.PrivacyEventDao
import com.v7lthronyx.scamynx.data.db.ScanDao
import com.v7lthronyx.scamynx.data.db.ScanDatabase
import com.v7lthronyx.scamynx.data.db.ScanDatabaseMigrations
import com.v7lthronyx.scamynx.data.db.ThreatFeedDao
import com.v7lthronyx.scamynx.data.darkwebmonitoring.DarkWebMonitoringDao
import com.v7lthronyx.scamynx.data.qrcode.QRCodeHistoryRoomDao
import com.v7lthronyx.scamynx.data.preferences.SettingsDataSource
import com.v7lthronyx.scamynx.data.preferences.SettingsPreferences
import com.v7lthronyx.scamynx.data.preferences.SettingsPreferencesSerializer
import com.v7lthronyx.scamynx.data.realtimeprotection.BlockedThreatsRoomDao
import dagger.Module
import dagger.Provides
import com.v7lthronyx.scamynx.data.db.TelemetryEventDao
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

private const val DATABASE_NAME = "scamynx.db"
private const val SETTINGS_FILE_NAME = "scamynx_settings.json"

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideScanDatabase(
        @ApplicationContext context: Context,
    ): ScanDatabase = Room.databaseBuilder(
        context,
        ScanDatabase::class.java,
        DATABASE_NAME,
    ).addMigrations(
        ScanDatabaseMigrations.MIGRATION_3_4,
        ScanDatabaseMigrations.MIGRATION_4_5,
        ScanDatabaseMigrations.MIGRATION_5_6,
        ScanDatabaseMigrations.MIGRATION_6_7,
        ScanDatabaseMigrations.MIGRATION_7_8,
    ).fallbackToDestructiveMigrationOnDowngrade()
        .build()

    @Provides
    fun provideScanDao(database: ScanDatabase): ScanDao = database.scanDao()
    @Provides
    fun provideThreatFeedDao(database: ScanDatabase): ThreatFeedDao = database.threatFeedDao()

    @Provides
    fun provideDarkWebMonitoringDao(database: ScanDatabase): DarkWebMonitoringDao = 
        database.darkWebMonitoringDao()

    @Provides
    fun providePrivacyEventDao(database: ScanDatabase): PrivacyEventDao =
        database.privacyEventDao()

    @Provides
    fun providePrivacyBaselineDao(database: ScanDatabase): PrivacyBaselineDao =
        database.privacyBaselineDao()

    @Provides
    fun provideQrCodeHistoryDao(database: ScanDatabase): QRCodeHistoryRoomDao =
        database.qrCodeHistoryDao()

    @Provides
    fun provideBlockedThreatsDao(database: ScanDatabase): BlockedThreatsRoomDao =
        database.blockedThreatsDao()

    @Provides
    @Singleton
    fun provideSettingsSerializer(
        @ThreatIntelJson json: Json,
    ): SettingsPreferencesSerializer = SettingsPreferencesSerializer(json)

    @Provides
    fun provideTelemetryEventDao(database: ScanDatabase): TelemetryEventDao =
        database.telemetryEventDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        serializer: SettingsPreferencesSerializer,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): DataStore<SettingsPreferences> = DataStoreFactory.create(
        serializer = serializer,
        corruptionHandler = ReplaceFileCorruptionHandler { SettingsPreferences() },
        produceFile = { context.dataStoreFile(SETTINGS_FILE_NAME) },
        scope = CoroutineScope(dispatcher + SupervisorJob()),
    )

    @Provides
    @Singleton
    fun provideSettingsDataSource(
        dataStore: DataStore<SettingsPreferences>,
    ): SettingsDataSource = SettingsDataSource(dataStore)
}

package com.v7lthronyx.scamynx.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.v7lthronyx.scamynx.data.db.ScanDao
import com.v7lthronyx.scamynx.data.db.ScanDatabase
import com.v7lthronyx.scamynx.data.preferences.SettingsDataSource
import com.v7lthronyx.scamynx.data.preferences.SettingsPreferences
import com.v7lthronyx.scamynx.data.preferences.SettingsPreferencesSerializer
import com.v7lthronyx.scamynx.data.db.ThreatFeedDao
import dagger.Module
import dagger.Provides
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
    ).fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideScanDao(database: ScanDatabase): ScanDao = database.scanDao()

    @Provides
    fun provideThreatFeedDao(database: ScanDatabase): ThreatFeedDao = database.threatFeedDao()

    @Provides
    @Singleton
    fun provideSettingsSerializer(
        @ThreatIntelJson json: Json,
    ): SettingsPreferencesSerializer = SettingsPreferencesSerializer(json)

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

package com.v7lthronyx.scamynx.data.preferences

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataSource @Inject constructor(
    private val dataStore: DataStore<SettingsPreferences>,
) {
    val data: Flow<SettingsPreferences> = dataStore.data

    suspend fun update(transform: (SettingsPreferences) -> SettingsPreferences) {
        dataStore.updateData(transform)
    }
}

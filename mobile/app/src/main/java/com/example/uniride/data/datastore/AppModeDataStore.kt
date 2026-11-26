package com.example.uniride.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.repository.AppModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Last mode chosen with the selector, in DataStore Preferences. It is not sensitive, so it is not
 * encrypted. Nothing saved, or a value that cannot be read, counts as [AppMode.PASAJERO]. Logging
 * out clears it with the rest of the preferences.
 */
class AppModeDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppModeRepository {

    override fun observeAppMode(): Flow<AppMode> =
        dataStore.data.map { prefs ->
            AppMode.entries.firstOrNull { it.name == prefs[APP_MODE] } ?: AppMode.PASAJERO
        }.distinctUntilChanged()

    override suspend fun setAppMode(mode: AppMode) {
        dataStore.edit { it[APP_MODE] = mode.name }
    }

    private companion object {
        val APP_MODE = stringPreferencesKey("app_mode")
    }
}

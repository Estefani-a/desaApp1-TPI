package com.example.uniride.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.uniride.core.dispatcher.DispatchersProvider
import com.example.uniride.data.local.database.UniRideDatabase
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clears all local user-scoped persistence on logout or HTTP 401 (CONVENCIONES §6.4):
 * Room tables (`clearAllTables()`) and non-sensitive DataStore preferences.
 */
fun interface LocalSessionCleaner {
    suspend fun clearAll()
}

@Singleton
class RoomLocalSessionCleaner @Inject constructor(
    private val database: UniRideDatabase,
    private val preferencesDataStore: DataStore<Preferences>,
    private val dispatchers: DispatchersProvider,
) : LocalSessionCleaner {

    override suspend fun clearAll() {
        withContext(dispatchers.io) {
            database.clearAllTables()
        }
        preferencesDataStore.edit { it.clear() }
    }
}

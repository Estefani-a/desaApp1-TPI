package com.example.uniride.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.uniride.domain.repository.NotificationPermissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Remembers in DataStore Preferences whether the contextual `POST_NOTIFICATIONS` prompt (RNF08)
 * has already been presented. Non-sensitive preference, cleared with the rest on logout.
 */
class NotificationPermissionDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : NotificationPermissionRepository {

    override fun observeHasPrompted(): Flow<Boolean> =
        dataStore.data
            .map { prefs -> prefs[PROMPTED] == true }
            .distinctUntilChanged()

    override suspend fun markPrompted() {
        dataStore.edit { it[PROMPTED] = true }
    }

    private companion object {
        val PROMPTED = booleanPreferencesKey("notification_permission_prompted")
    }
}

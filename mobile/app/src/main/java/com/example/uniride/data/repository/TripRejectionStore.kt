package com.example.uniride.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.TripId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Why the server rejected a trip that was published offline. It lives in DataStore (not in
 * Room) so it survives restarts without changing the schema. Each entry is `tripId|DATA_ERROR`.
 */
class TripRejectionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    fun observe(): Flow<Map<TripId, DataError>> =
        dataStore.data.map { prefs -> prefs[KEY].orEmpty().mapNotNull(::parse).toMap() }.distinctUntilChanged()

    /** Ids of the trips with a reason that the user did not dismiss yet. */
    suspend fun ids(): Set<String> = observe().first().keys.mapTo(HashSet()) { it.value }

    suspend fun add(tripId: TripId, error: DataError) {
        dataStore.edit { prefs ->
            prefs[KEY] = prefs[KEY].orEmpty().withoutTrip(tripId) + "${tripId.value}$SEPARATOR${error.name}"
        }
    }

    suspend fun remove(tripId: TripId) {
        dataStore.edit { prefs -> prefs[KEY] = prefs[KEY].orEmpty().withoutTrip(tripId) }
    }

    private fun Set<String>.withoutTrip(tripId: TripId): Set<String> =
        filterNot { it.startsWith(tripId.value + SEPARATOR) }.toSet()

    private fun parse(entry: String): Pair<TripId, DataError>? {
        val id = entry.substringBefore(SEPARATOR)
        val error = DataError.entries.firstOrNull { it.name == entry.substringAfter(SEPARATOR, "") }
        return if (id.isEmpty() || error == null) null else TripId(id) to error
    }

    private companion object {
        const val SEPARATOR = "|"
        val KEY = stringSetPreferencesKey("trip_rejections")
    }
}

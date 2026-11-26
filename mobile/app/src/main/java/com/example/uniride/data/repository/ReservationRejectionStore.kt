package com.example.uniride.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationRejection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Why the server did not accept a reservation change made on this device. Same approach as
 * [TripRejectionStore]: DataStore, so it survives restarts without changing the Room schema.
 * Each entry is `reservationId|REJECTION`.
 */
class ReservationRejectionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    fun observe(): Flow<Map<ReservationId, ReservationRejection>> =
        dataStore.data.map { prefs -> prefs[KEY].orEmpty().mapNotNull(::parse).toMap() }.distinctUntilChanged()

    suspend fun add(reservationId: ReservationId, rejection: ReservationRejection) {
        dataStore.edit { prefs ->
            prefs[KEY] = prefs[KEY].orEmpty().without(reservationId) + "${reservationId.value}$SEPARATOR${rejection.name}"
        }
    }

    suspend fun remove(reservationId: ReservationId) {
        dataStore.edit { prefs -> prefs[KEY] = prefs[KEY].orEmpty().without(reservationId) }
    }

    private fun Set<String>.without(reservationId: ReservationId): Set<String> =
        filterNot { it.startsWith(reservationId.value + SEPARATOR) }.toSet()

    private fun parse(entry: String): Pair<ReservationId, ReservationRejection>? {
        val id = entry.substringBefore(SEPARATOR)
        val rejection = ReservationRejection.entries.firstOrNull { it.name == entry.substringAfter(SEPARATOR, "") }
        return if (id.isEmpty() || rejection == null) null else ReservationId(id) to rejection
    }

    private companion object {
        const val SEPARATOR = "|"
        val KEY = stringSetPreferencesKey("reservation_rejections")
    }
}

package com.example.uniride.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.Shift
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.repository.FeedFiltersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Last filters of the feed in DataStore Preferences (RF03). They are not sensitive, so they are
 * not encrypted. A value that cannot be read (for example, a renamed enum) counts as "any".
 */
class FeedFiltersDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FeedFiltersRepository {

    override fun observeLastFilters(): Flow<TripFilters> =
        dataStore.data.map { prefs ->
            TripFilters(
                campus = prefs[CAMPUS]?.let(::CampusCode),
                direction = prefs[DIRECTION]?.let { enumOrNull<Direction>(it) },
                date = prefs[DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                shift = prefs[SHIFT]?.let { enumOrNull<Shift>(it) },
            )
        }.distinctUntilChanged()

    override suspend fun saveLastFilters(filters: TripFilters) {
        dataStore.edit { prefs ->
            prefs.setOrRemove(CAMPUS, filters.campus?.value)
            prefs.setOrRemove(DIRECTION, filters.direction?.name)
            prefs.setOrRemove(DATE, filters.date?.toString())
            prefs.setOrRemove(SHIFT, filters.shift?.name)
        }
    }

    private fun MutablePreferences.setOrRemove(
        key: Preferences.Key<String>,
        value: String?,
    ) {
        if (value == null) remove(key) else this[key] = value
    }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? =
        enumValues<E>().firstOrNull { it.name == name }

    private companion object {
        val CAMPUS = stringPreferencesKey("feed_filter_campus")
        val DIRECTION = stringPreferencesKey("feed_filter_direction")
        val DATE = stringPreferencesKey("feed_filter_date")
        val SHIFT = stringPreferencesKey("feed_filter_shift")
    }
}

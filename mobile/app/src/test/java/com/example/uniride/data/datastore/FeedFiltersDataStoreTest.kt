package com.example.uniride.data.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.Shift
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.fake.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** RF03: the last filters survive closing the app. */
class FeedFiltersDataStoreTest {

    private fun dataStore() = InMemoryPreferencesDataStore()

    @Test
    fun `sin nada guardado no hay filtros`() = runTest {
        assertEquals(TripFilters(), FeedFiltersDataStore(dataStore()).observeLastFilters().first())
    }

    @Test
    fun `los filtros guardados se leen iguales`() = runTest {
        val repository = FeedFiltersDataStore(dataStore())
        val filters = TripFilters(CampusCode("MONSERRAT"), Direction.VUELTA, LocalDate.of(2026, 11, 20), Shift.NOCHE)

        repository.saveLastFilters(filters)

        assertEquals(filters, repository.observeLastFilters().first())
    }

    @Test
    fun `quitar un filtro lo borra`() = runTest {
        val repository = FeedFiltersDataStore(dataStore())
        repository.saveLastFilters(TripFilters(campus = CampusCode("MONSERRAT"), shift = Shift.TARDE))

        repository.saveLastFilters(TripFilters(shift = Shift.TARDE))

        assertEquals(TripFilters(shift = Shift.TARDE), repository.observeLastFilters().first())
    }

    @Test
    fun `un valor que no se puede leer cuenta como cualquiera`() = runTest {
        val store = dataStore()
        store.edit {
            it[stringPreferencesKey("feed_filter_shift")] = "MADRUGADA"
            it[stringPreferencesKey("feed_filter_date")] = "ayer"
        }

        assertEquals(TripFilters(), FeedFiltersDataStore(store).observeLastFilters().first())
    }
}

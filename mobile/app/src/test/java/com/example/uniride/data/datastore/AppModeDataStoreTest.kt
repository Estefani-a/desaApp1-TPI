package com.example.uniride.data.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.uniride.domain.model.AppMode
import com.example.uniride.fake.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The mode chosen with the selector survives closing the app. */
class AppModeDataStoreTest {

    @Test
    fun `sin nada guardado es pasajero`() = runTest {
        assertEquals(AppMode.PASAJERO, AppModeDataStore(InMemoryPreferencesDataStore()).observeAppMode().first())
    }

    @Test
    fun `el modo guardado se lee igual`() = runTest {
        val repository = AppModeDataStore(InMemoryPreferencesDataStore())

        repository.setAppMode(AppMode.CONDUCTOR)

        assertEquals(AppMode.CONDUCTOR, repository.observeAppMode().first())
    }

    @Test
    fun `un valor que no se puede leer es pasajero`() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit { it[stringPreferencesKey("app_mode")] = "AMBOS" }

        assertEquals(AppMode.PASAJERO, AppModeDataStore(store).observeAppMode().first())
    }
}

package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.data.remote.datasource.FakeTripRemoteDataSource
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.model.TripId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.ZoneId

/** The provisional repository of RF03 against the fake backend of A2 and its demo trips. */
class InMemoryTripRepositoryTest {

    private val store = FakeBackendStore().apply { currentUserId = FakeBackendStore.DEFAULT_PASSENGER_ID }
    private val remote = FakeTripRemoteDataSource(store)
    private val repository = InMemoryTripRepository(remote, Clock.system(ZoneId.of("America/Argentina/Buenos_Aires")))

    @Test
    fun `antes del primer refresco no hay viajes ni hora de actualizacion`() = runTest {
        assertTrue(repository.observeTrips(TripFilters()).first().isEmpty())
        assertNull(repository.observeLastRefreshedAt().first())
    }

    @Test
    fun `refrescar trae los viajes y guarda la hora`() = runTest {
        assertEquals(AppResult.Success(Unit), repository.refreshTrips())

        assertEquals(3, repository.observeTrips(TripFilters()).first().size)
        assertNotNull(repository.observeLastRefreshedAt().first())
    }

    @Test
    fun `los filtros se aplican sobre lo guardado`() = runTest {
        repository.refreshTrips()

        val belgrano = repository.observeTrips(TripFilters(campus = CampusCode("BELGRANO"))).first()

        assertEquals(listOf(FakeBackendStore.DEMO_TRIP_BELGRANO_ID), belgrano.map { it.id.value })
    }

    @Test
    fun `un refresco fallido no borra lo guardado`() = runTest {
        repository.refreshTrips()
        store.currentUserId = null

        assertEquals(AppResult.Failure(DataError.UNAUTHORIZED), repository.refreshTrips())
        assertEquals(3, repository.observeTrips(TripFilters()).first().size)
    }

    @Test
    fun `el refresco parcial saca un viaje que el conductor cancelo`() = runTest {
        repository.refreshTrips()
        store.currentUserId = FakeBackendStore.DEFAULT_DRIVER_ID
        remote.cancelTrip(FakeBackendStore.DEMO_TRIP_RECOLETA_ID)
        store.currentUserId = FakeBackendStore.DEFAULT_PASSENGER_ID

        repository.refreshTrips()

        val ids = repository.observeTrips(TripFilters()).first().map { it.id }
        assertEquals(2, ids.size)
        assertTrue(TripId(FakeBackendStore.DEMO_TRIP_RECOLETA_ID) !in ids)
    }
}

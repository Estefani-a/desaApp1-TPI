package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.remote.datasource.FakeBackendStore
import com.example.uniride.data.remote.datasource.FakeTripRemoteDataSource
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.Shift
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripRoute
import com.example.uniride.domain.model.TripSchedule
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.fake.FakeSyncScheduler
import com.example.uniride.fake.FakeTripDao
import com.example.uniride.fake.FakeUserDao
import com.example.uniride.fake.InMemoryPreferencesDataStore
import com.example.uniride.fake.ToggleableTripRemote
import com.example.uniride.fake.tripRow
import com.example.uniride.fake.userRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** [TripRepositoryImpl] over fake DAOs, a fixed clock and the fake backend of A2 with its demo trips. */
class TripRepositoryImplTest {

    private val store = FakeBackendStore().apply { currentUserId = FakeBackendStore.DEFAULT_PASSENGER_ID }
    private val remote = ToggleableTripRemote(FakeTripRemoteDataSource(store))
    private val tripDao = FakeTripDao()
    private val userDao = FakeUserDao()
    private val scheduler = FakeSyncScheduler()
    private val dataStore = InMemoryPreferencesDataStore()
    private val rejections = TripRejectionStore(dataStore)
    private val now = Instant.now()
    private val clock = Clock.fixed(now, ZoneId.of("America/Argentina/Buenos_Aires"))
    private val repository = newRepository()

    private fun newRepository() =
        TripRepositoryImpl(tripDao, userDao, remote, rejections, scheduler, dataStore, clock)

    private fun draft(seats: Int = 3) = TripDraft(
        campus = CampusCode("MONSERRAT"),
        route = TripRoute(Direction.VUELTA, "Salida Lima 717", "Ramos Mejía", null),
        schedule = TripSchedule(LocalDate.now().plusDays(2), LocalTime.of(22, 15), null),
        totalSeats = seats,
        contributionPerSeat = 3500,
    )

    // --- Refresh ---

    @Test
    fun `antes del primer refresco no hay viajes ni hora de actualizacion`() = runTest {
        assertTrue(repository.observeTrips(TripFilters()).first().isEmpty())
        assertNull(repository.observeLastRefreshedAt().first())
    }

    @Test
    fun `refrescar trae los viajes y guarda la hora`() = runTest {
        assertEquals(AppResult.Success(Unit), repository.refreshTrips())

        assertEquals(3, repository.observeTrips(TripFilters()).first().size)
        assertEquals(now.toEpochMilli(), repository.observeLastRefreshedAt().first()?.toEpochMilli())
    }

    @Test
    fun `la hora de actualizacion sobrevive a un repositorio nuevo`() = runTest {
        repository.refreshTrips()

        assertEquals(now.toEpochMilli(), newRepository().observeLastRefreshedAt().first()?.toEpochMilli())
    }

    @Test
    fun `los filtros se aplican sobre lo guardado`() = runTest {
        repository.refreshTrips()

        val belgrano = repository.observeTrips(TripFilters(campus = CampusCode("BELGRANO"))).first()

        assertEquals(listOf(FakeBackendStore.DEMO_TRIP_BELGRANO_ID), belgrano.map { it.id.value })
    }

    @Test
    fun `el filtro por turno lo aplica el repositorio`() = runTest {
        tripDao.upsertAll(listOf(tripRow("noche", time = LocalTime.of(22, 15)), tripRow("manana", time = LocalTime.of(8, 0))))

        val night = repository.observeTrips(TripFilters(shift = Shift.NOCHE)).first()

        assertEquals(listOf("noche"), night.map { it.id.value })
    }

    @Test
    fun `un refresco fallido no borra lo guardado ni la hora`() = runTest {
        repository.refreshTrips()
        store.currentUserId = null

        assertEquals(AppResult.Failure(DataError.UNAUTHORIZED), repository.refreshTrips())
        assertEquals(3, repository.observeTrips(TripFilters()).first().size)
        assertEquals(now.toEpochMilli(), repository.observeLastRefreshedAt().first()?.toEpochMilli())
    }

    @Test
    fun `sin conexion el refresco devuelve el error y deja lo guardado`() = runTest {
        repository.refreshTrips()
        remote.failWith = IOException()

        assertEquals(AppResult.Failure(DataError.NO_CONNECTION), repository.refreshTrips())
        assertEquals(3, repository.observeTrips(TripFilters()).first().size)
    }

    @Test
    fun `el refresco parcial saca un viaje que el conductor cancelo`() = runTest {
        repository.refreshTrips()
        cancelAsDriver(FakeBackendStore.DEMO_TRIP_RECOLETA_ID)

        repository.refreshTrips()

        val ids = repository.observeTrips(TripFilters()).first().map { it.id }
        assertEquals(2, ids.size)
        assertTrue(TripId(FakeBackendStore.DEMO_TRIP_RECOLETA_ID) !in ids)
        assertNull(repository.observeTrip(TripId(FakeBackendStore.DEMO_TRIP_RECOLETA_ID)).first())
    }

    @Test
    fun `el refresco parcial conserva un viaje cancelado donde el usuario tiene reserva activa`() = runTest {
        userDao.user.value = userRow(FakeBackendStore.DEFAULT_PASSENGER_ID, withVehicle = false)
        tripDao.activeReservationTripIds[FakeBackendStore.DEFAULT_PASSENGER_ID] = setOf(FakeBackendStore.DEMO_TRIP_RECOLETA_ID)
        repository.refreshTrips()
        cancelAsDriver(FakeBackendStore.DEMO_TRIP_RECOLETA_ID)

        repository.refreshTrips()

        val kept = repository.observeTrip(TripId(FakeBackendStore.DEMO_TRIP_RECOLETA_ID)).first()
        assertEquals(TripStatus.CANCELADO, kept?.status)
        assertEquals(listOf(kept), repository.observeMyTrips().first().filter { it.id == kept?.id })
    }

    @Test
    fun `el refresco completo conserva lo pendiente y descarta lo que el servidor ya no lista`() = runTest {
        tripDao.upsertAll(
            listOf(
                tripRow("pendiente", isSync = false),
                tripRow("viejo", isSync = true),
            ),
        )

        repository.refreshTrips()

        val ids = tripDao.getAll().map { it.tripId }.toSet()
        assertTrue("pendiente" in ids)
        assertTrue("viejo" !in ids)
        assertEquals(4, ids.size)
    }

    @Test
    fun `el refresco conserva un viaje rechazado hasta que se descarte`() = runTest {
        tripDao.upsert(tripRow("rechazado", status = TripStatus.CANCELADO, isSync = true))
        rejections.add(TripId("rechazado"), DataError.VALIDATION)

        repository.refreshTrips()

        assertTrue(tripDao.getAll().any { it.tripId == "rechazado" })
        assertEquals(mapOf(TripId("rechazado") to DataError.VALIDATION), repository.observeRejectedTrips().first())

        repository.dismissRejection(TripId("rechazado"))

        assertTrue(tripDao.getAll().none { it.tripId == "rechazado" })
        assertTrue(repository.observeRejectedTrips().first().isEmpty())
    }

    @Test
    fun `refrescar un viaje y mis viajes los guarda sincronizados`() = runTest {
        store.currentUserId = FakeBackendStore.DEFAULT_DRIVER_ID
        userDao.user.value = userRow()

        assertEquals(AppResult.Success(Unit), repository.refreshTrip(TripId(FakeBackendStore.DEMO_TRIP_MONSERRAT_ID)))
        assertEquals(AppResult.Success(Unit), repository.refreshMyTrips())

        assertTrue(tripDao.getAll().all { it.isSync })
        assertEquals(2, repository.observeMyTrips().first().size)
    }

    @Test
    fun `mis viajes publicados son solo los del conductor, no los que reservo`() = runTest {
        userDao.user.value = userRow(FakeBackendStore.DEFAULT_DRIVER_ID)
        tripDao.activeReservationTripIds[FakeBackendStore.DEFAULT_DRIVER_ID] = setOf("reservado")
        tripDao.upsertAll(
            listOf(
                tripRow("propio", driverId = FakeBackendStore.DEFAULT_DRIVER_ID),
                tripRow("reservado", driverId = "otro-conductor"),
            ),
        )

        assertEquals(listOf(TripId("propio")), repository.observeMyPublishedTrips().first().map { it.id })
        assertEquals(2, repository.observeMyTrips().first().size)
    }

    @Test
    fun `sin usuario no hay viajes publicados`() = runTest {
        userDao.user.value = null
        tripDao.upsert(tripRow("propio"))

        assertTrue(repository.observeMyPublishedTrips().first().isEmpty())
    }

    @Test
    fun `borrar los datos locales vacia viajes, hora de refresco y motivos de rechazo`() = runTest {
        repository.refreshTrips()
        tripDao.upsert(tripRow("pendiente", isSync = false))
        rejections.add(TripId("rechazado"), DataError.VALIDATION)

        assertEquals(AppResult.Success(Unit), repository.clearLocalData())

        assertTrue(tripDao.getAll().isEmpty())
        assertNull(repository.observeLastRefreshedAt().first())
        assertTrue(repository.observeRejectedTrips().first().isEmpty())
    }

    @Test
    fun `despues de borrar los datos locales el refresco vuelve a ser completo`() = runTest {
        repository.refreshTrips()
        repository.clearLocalData()

        repository.refreshTrips()

        assertEquals(3, tripDao.getAll().size)
    }

    // --- Publish ---

    @Test
    fun `publicar sin conexion deja el viaje pendiente y pide sincronizar sin llamar a la red`() = runTest {
        userDao.user.value = userRow()
        remote.failWith = IOException()

        val result = repository.publish(draft())

        val id = (result as AppResult.Success).data
        val row = tripDao.getAll().single()
        assertEquals(id.value, row.tripId)
        assertEquals(false, row.isSync)
        assertEquals(3, row.availableSeats)
        assertEquals(TripStatus.ABIERTO, row.status)
        assertEquals("Lucía Fernández", row.driverName)
        assertEquals("AB123CD", row.vehiclePlate)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, remote.calls)
        assertEquals(setOf(id), repository.observePendingTripIds().first())
    }

    @Test
    fun `publicar sin usuario guardado devuelve UNAUTHORIZED y no guarda nada`() = runTest {
        assertEquals(AppResult.Failure(DataError.UNAUTHORIZED), repository.publish(draft()))

        assertTrue(tripDao.getAll().isEmpty())
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun `publicar sin vehiculo devuelve VALIDATION`() = runTest {
        userDao.user.value = userRow(withVehicle = false)

        assertEquals(AppResult.Failure(DataError.VALIDATION), repository.publish(draft()))
        assertTrue(tripDao.getAll().isEmpty())
    }

    // --- Finish and cancel ---

    @Test
    fun `finalizar guarda el viaje que devuelve el servidor`() = runTest {
        store.currentUserId = FakeBackendStore.DEFAULT_DRIVER_ID
        repository.refreshTrips()

        assertEquals(AppResult.Success(Unit), repository.finish(TripId(FakeBackendStore.DEMO_TRIP_MONSERRAT_ID)))

        val row = tripDao.getAll().first { it.tripId == FakeBackendStore.DEMO_TRIP_MONSERRAT_ID }
        assertEquals(TripStatus.FINALIZADO, row.status)
        assertTrue(row.isSync)
    }

    @Test
    fun `cancelar sin conexion devuelve el error y no cambia el viaje`() = runTest {
        store.currentUserId = FakeBackendStore.DEFAULT_DRIVER_ID
        repository.refreshTrips()
        val before = tripDao.getAll().toSet()
        remote.failWith = IOException()

        assertEquals(AppResult.Failure(DataError.NO_CONNECTION), repository.cancel(TripId(FakeBackendStore.DEMO_TRIP_MONSERRAT_ID)))

        assertEquals(before, tripDao.getAll().toSet())
    }

    private suspend fun cancelAsDriver(tripId: String) {
        val viewer = store.currentUserId
        store.currentUserId = FakeBackendStore.DEFAULT_DRIVER_ID
        remote.cancelTrip(tripId)
        store.currentUserId = viewer
    }
}

package com.example.uniride.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.uniride.data.local.database.UniRideDatabase
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.TripStatus
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDaoTest {

    private lateinit var database: UniRideDatabase
    private lateinit var dao: TripDao

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        dao = database.tripDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `un viaje guardado se observa por id y tambien devuelve null si no existe`() = runTest {
        val trip = tripEntity()

        dao.upsert(trip)

        assertEquals(trip, dao.observeTrip("trip-1").first())
        assertNull(dao.observeTrip("otro").first())
    }

    @Test
    fun `guardar el mismo viaje con otros datos lo actualiza`() = runTest {
        dao.upsert(tripEntity())

        dao.upsert(tripEntity().copy(availableSeats = 0))

        assertEquals(0, dao.observeTrip("trip-1").first()?.availableSeats)
    }

    @Test
    fun `observar viajes abiertos devuelve solo los abiertos ordenados por salida`() = runTest {
        dao.upsertAll(
            listOf(
                tripEntity(id = "tarde", time = LocalTime.of(22, 15)),
                tripEntity(id = "temprano", time = LocalTime.of(9, 0)),
                tripEntity(id = "completo", status = TripStatus.COMPLETO),
                tripEntity(id = "cancelado", status = TripStatus.CANCELADO),
            ),
        )

        val ids = dao.observeOpenTrips(null, null, null).first().map { it.tripId }

        assertEquals(listOf("temprano", "tarde"), ids)
    }

    @Test
    fun `los filtros de sede sentido y fecha se combinan y null significa cualquiera`() = runTest {
        dao.upsertAll(
            listOf(
                tripEntity(id = "a", campus = "MONSERRAT", direction = Direction.VUELTA),
                tripEntity(id = "b", campus = "MONSERRAT", direction = Direction.IDA),
                tripEntity(id = "c", campus = "RECOLETA", direction = Direction.VUELTA),
                tripEntity(id = "d", campus = "MONSERRAT", direction = Direction.VUELTA, date = LocalDate.of(2026, 11, 21)),
            ),
        )

        val bySede = dao.observeOpenTrips("MONSERRAT", null, null).first().map { it.tripId }
        val bySedeYSentido = dao.observeOpenTrips("MONSERRAT", Direction.VUELTA, null).first().map { it.tripId }
        val todo = dao.observeOpenTrips("MONSERRAT", Direction.VUELTA, LocalDate.of(2026, 11, 20)).first()

        assertEquals(listOf("a", "b", "d"), bySede.sorted())
        assertEquals(listOf("a", "d"), bySedeYSentido.sorted())
        assertEquals(listOf("a"), todo.map { it.tripId })
    }

    @Test
    fun `observar los viajes de un conductor devuelve solo los suyos`() = runTest {
        dao.upsertAll(
            listOf(
                tripEntity(id = "mio-1", driverId = "yo"),
                tripEntity(id = "mio-2", driverId = "yo", status = TripStatus.CANCELADO),
                tripEntity(id = "ajeno", driverId = "otro"),
            ),
        )

        val ids = dao.observeTripsByDriver("yo").first().map { it.tripId }

        assertEquals(listOf("mio-1", "mio-2"), ids.sorted())
    }

    @Test
    fun `los viajes con isSync false aparecen como pendientes`() = runTest {
        dao.upsertAll(
            listOf(
                tripEntity(id = "confirmado", isSync = true),
                tripEntity(id = "pendiente", isSync = false),
            ),
        )

        assertEquals(listOf("pendiente"), dao.observePendingTripIds().first())
        assertEquals(listOf("pendiente"), dao.getPendingTrips().map { it.tripId })
    }

    @Test
    fun `markSynced saca al viaje de los pendientes sin tocar los demas datos`() = runTest {
        dao.upsertAll(listOf(tripEntity(id = "a", isSync = false), tripEntity(id = "b", isSync = false)))

        dao.markSynced("a")

        assertEquals(listOf("b"), dao.observePendingTripIds().first())
        assertEquals(tripEntity(id = "a", isSync = true), dao.observeTrip("a").first())
    }

    @Test
    fun `la lista de pendientes observada se actualiza cuando cambia la base`() = runTest {
        val flow = dao.observePendingTripIds()
        assertEquals(emptyList<String>(), flow.first())

        dao.upsert(tripEntity(isSync = false))

        assertEquals(listOf("trip-1"), flow.first())
    }

    @Test
    fun `replaceCatalog completo conserva lo pendiente y lo que tiene reserva activa del usuario`() = runTest {
        database.reservationDao().upsert(reservationEntity(id = "r1", tripId = "reservado", passengerId = "yo"))
        dao.upsertAll(
            listOf(
                tripEntity(id = "pendiente", isSync = false),
                tripEntity(id = "reservado"),
                tripEntity(id = "ajeno"),
                tripEntity(id = "viejo"),
            ),
        )

        dao.replaceCatalog(listOf(tripEntity(id = "nuevo")), full = true, userId = "yo", keepTripIds = setOf("ajeno"))

        assertEquals(setOf("nuevo", "pendiente", "reservado", "ajeno"), dao.getAll().map { it.tripId }.toSet())
    }

    @Test
    fun `replaceCatalog parcial saca los finalizados salvo los que el usuario tiene reservados`() = runTest {
        database.reservationDao().upsert(reservationEntity(id = "r1", tripId = "reservado", passengerId = "yo"))
        dao.upsertAll(listOf(tripEntity(id = "a"), tripEntity(id = "b"), tripEntity(id = "reservado")))

        dao.replaceCatalog(
            listOf(
                tripEntity(id = "a", status = TripStatus.CANCELADO),
                tripEntity(id = "reservado", status = TripStatus.FINALIZADO),
                tripEntity(id = "c"),
            ),
            full = false,
            userId = "yo",
            keepTripIds = emptySet(),
        )

        val byId = dao.getAll().associateBy { it.tripId }
        assertEquals(setOf("b", "reservado", "c"), byId.keys)
        assertEquals(TripStatus.FINALIZADO, byId.getValue("reservado").status)
    }

    @Test
    fun `observeMyTrips trae los que ofrece el usuario y los que tiene reservados`() = runTest {
        database.reservationDao().upsert(reservationEntity(id = "r1", tripId = "reservado", passengerId = "yo"))
        dao.upsertAll(
            listOf(
                tripEntity(id = "propio", driverId = "yo"),
                tripEntity(id = "reservado"),
                tripEntity(id = "ajeno"),
            ),
        )

        assertEquals(setOf("propio", "reservado"), dao.observeMyTrips("yo").first().map { it.tripId }.toSet())
    }

    @Test
    fun `observeListedTrips incluye completos y deja afuera finalizados y cancelados`() = runTest {
        dao.upsertAll(
            listOf(
                tripEntity(id = "abierto"),
                tripEntity(id = "completo", status = TripStatus.COMPLETO),
                tripEntity(id = "finalizado", status = TripStatus.FINALIZADO),
                tripEntity(id = "cancelado", status = TripStatus.CANCELADO),
            ),
        )

        assertEquals(setOf("abierto", "completo"), dao.observeListedTrips().first().map { it.tripId }.toSet())
    }
}

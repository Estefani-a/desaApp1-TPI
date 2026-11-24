package com.example.uniride.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.uniride.data.local.database.UniRideDatabase
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReservationDaoTest {

    private lateinit var database: UniRideDatabase
    private lateinit var dao: ReservationDao

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        dao = database.reservationDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `una reserva guardada se observa por id y devuelve null si no existe`() = runTest {
        val reservation = reservationEntity()

        dao.upsert(reservation)

        assertEquals(reservation, dao.observeReservation("res-1").first())
        assertNull(dao.observeReservation("otra").first())
    }

    @Test
    fun `se puede guardar una reserva sin que exista el viaje`() = runTest {
        dao.upsert(reservationEntity(tripId = "viaje-que-no-esta-en-la-base"))

        assertEquals(1, dao.observeByTrip("viaje-que-no-esta-en-la-base").first().size)
    }

    @Test
    fun `observar por viaje devuelve solo las reservas de ese viaje`() = runTest {
        dao.upsertAll(
            listOf(
                reservationEntity(id = "a", tripId = "trip-1"),
                reservationEntity(id = "b", tripId = "trip-1"),
                reservationEntity(id = "c", tripId = "trip-2"),
            ),
        )

        val ids = dao.observeByTrip("trip-1").first().map { it.reservationId }

        assertEquals(listOf("a", "b"), ids.sorted())
    }

    @Test
    fun `observar por pasajero devuelve las suyas con la mas nueva primero`() = runTest {
        val base = Instant.parse("2026-11-20T21:00:00Z")
        dao.upsertAll(
            listOf(
                reservationEntity(id = "vieja", reservedAt = base),
                reservationEntity(id = "nueva", reservedAt = base.plusSeconds(3600)),
                reservationEntity(id = "ajena", passengerId = "otro"),
            ),
        )

        val ids = dao.observeByPassenger("passenger-1").first().map { it.reservationId }

        assertEquals(listOf("nueva", "vieja"), ids)
    }

    @Test
    fun `las reservas con isSync false aparecen como pendientes`() = runTest {
        dao.upsertAll(
            listOf(
                reservationEntity(id = "confirmada", isSync = true),
                reservationEntity(id = "pendiente", isSync = false),
            ),
        )

        assertEquals(listOf("pendiente"), dao.observePendingReservationIds().first())
        assertEquals(listOf("pendiente"), dao.getPendingReservations().map { it.reservationId })
    }

    @Test
    fun `markSynced saca a la reserva de los pendientes`() = runTest {
        dao.upsertAll(listOf(reservationEntity(id = "a", isSync = false), reservationEntity(id = "b", isSync = false)))

        dao.markSynced("a")

        assertEquals(listOf("b"), dao.observePendingReservationIds().first())
        assertEquals(reservationEntity(id = "a", isSync = true), dao.observeReservation("a").first())
    }
}

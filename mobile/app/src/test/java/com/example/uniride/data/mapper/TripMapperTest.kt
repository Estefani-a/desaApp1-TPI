package com.example.uniride.data.mapper

import com.example.uniride.data.remote.dto.CampusDto
import com.example.uniride.data.remote.dto.TripDto
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.Shift
import com.example.uniride.domain.model.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** TripDto of docs/API.md §3.3 → domain. */
class TripMapperTest {

    private val dto = TripDto(
        tripId = "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31",
        driverId = "6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10",
        driverName = "Lucía F.",
        driverRating = 4.8,
        direction = "VUELTA",
        campus = "MONSERRAT",
        meetingPoint = "Salida Lima 717",
        destinationNeighborhood = "Ramos Mejía",
        destinationReference = "Estación de tren",
        departureDate = "2026-11-20",
        departureTime = "22:15",
        estimatedArrivalTime = "23:00",
        totalSeats = 3,
        availableSeats = 2,
        contributionPerSeat = 3500,
        vehicleModel = "Gol Trend",
        vehicleColor = "Gris",
        vehiclePlate = null,
        driverPhone = null,
        status = "ABIERTO",
        myReservation = null,
        updatedAt = "2026-11-20T21:40:12-03:00",
    )

    @Test
    fun `el ejemplo del contrato se lee completo`() {
        val trip = dto.toDomain()

        assertEquals("0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31", trip.id.value)
        assertEquals("Lucía F.", trip.driver.displayName)
        assertEquals(4.8, trip.driver.rating)
        assertEquals(CampusCode("MONSERRAT"), trip.campus)
        assertEquals(Direction.VUELTA, trip.route.direction)
        assertEquals("Estación de tren", trip.route.destinationReference)
        assertEquals(LocalDate.of(2026, 11, 20), trip.schedule.departureDate)
        assertEquals(LocalTime.of(22, 15), trip.schedule.departureTime)
        assertEquals(Shift.NOCHE, trip.schedule.shift)
        assertEquals(3500, trip.contributionPerSeat)
        assertEquals(TripStatus.ABIERTO, trip.status)
        assertEquals(Instant.parse("2026-11-21T00:40:12Z"), trip.updatedAt)
    }

    @Test
    fun `sin reserva aceptada no hay patente ni telefono`() {
        val trip = dto.toDomain()

        assertNull(trip.vehicle.plate)
        assertNull(trip.driver.phone)
    }

    @Test
    fun `los opcionales en null se respetan`() {
        val trip = dto.copy(estimatedArrivalTime = null, destinationReference = null, driverRating = null).toDomain()

        assertNull(trip.schedule.estimatedArrivalTime)
        assertNull(trip.route.destinationReference)
        assertNull(trip.driver.rating)
    }

    @Test
    fun `la sede conserva sus puntos de encuentro en orden`() {
        val campus = CampusDto("BELGRANO", "Sede Belgrano", listOf("Salida 11 de Septiembre 1990", "Salida Echeverría"))
            .toDomain()

        assertEquals(CampusCode("BELGRANO"), campus.code)
        assertEquals(listOf("Salida 11 de Septiembre 1990", "Salida Echeverría"), campus.meetingPoints)
    }
}

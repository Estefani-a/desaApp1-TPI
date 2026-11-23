package com.example.uniride.domain.model

import com.example.uniride.domain.model.DomainFixtures.trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DomainModelTest {

    @Test
    fun `el turno usa las franjas acordadas en A0`() {
        assertEquals(Shift.NOCHE, Shift.of(LocalTime.of(0, 0)))
        assertEquals(Shift.NOCHE, Shift.of(LocalTime.of(5, 59)))
        assertEquals(Shift.MANANA, Shift.of(LocalTime.of(6, 0)))
        assertEquals(Shift.MANANA, Shift.of(LocalTime.of(12, 59, 59)))
        assertEquals(Shift.TARDE, Shift.of(LocalTime.of(13, 0)))
        assertEquals(Shift.TARDE, Shift.of(LocalTime.of(17, 59)))
        assertEquals(Shift.NOCHE, Shift.of(LocalTime.of(18, 0)))
        assertEquals(Shift.NOCHE, Shift.of(LocalTime.of(23, 59)))
    }

    @Test
    fun `filtros vacios aceptan cualquier viaje`() {
        assertTrue(TripFilters().isEmpty)
        assertTrue(TripFilters().matches(trip()))
    }

    @Test
    fun `los filtros se combinan con Y`() {
        val filters = TripFilters(
            campus = CampusCode("MONSERRAT"),
            direction = Direction.VUELTA,
            date = LocalDate.of(2026, 11, 20),
            shift = Shift.NOCHE,
        )

        assertTrue(filters.matches(trip()))
        assertFalse(filters.matches(trip(campus = CampusCode("BELGRANO"))))
        assertFalse(filters.matches(trip(direction = Direction.IDA)))
        assertFalse(filters.matches(trip(date = LocalDate.of(2026, 11, 21))))
        assertFalse(filters.matches(trip(time = LocalTime.of(9, 0))))
    }

    @Test
    fun `una reserva activa es la pendiente o la aceptada`() {
        assertTrue(ReservationStatus.PENDIENTE.isActive)
        assertTrue(ReservationStatus.ACEPTADA.isActive)
        assertFalse(ReservationStatus.RECHAZADA.isActive)
        assertFalse(ReservationStatus.CANCELADA.isActive)
    }

    @Test
    fun `solo puede manejar quien cargo un vehiculo`() {
        val profile = Profile("Lucía Fernández", "Ingeniería", "+5491155550000", PreferredMode.AMBOS, vehicle = null)
        val user = User(UserId("user-1"), "conductora.demo@uade.edu.ar", profile, rating = null)

        assertFalse(user.canDrive)
        assertTrue(user.copy(profile = profile.copy(vehicle = Vehicle("VW", "Gol Trend", "Gris", "AB123CD"))).canDrive)
    }

    @Test
    fun `los enums tienen los valores del modelo de la preentrega`() {
        assertEquals(listOf("IDA", "VUELTA"), Direction.entries.map { it.name })
        assertEquals(listOf("ABIERTO", "COMPLETO", "FINALIZADO", "CANCELADO"), TripStatus.entries.map { it.name })
        assertEquals(
            listOf("PENDIENTE", "ACEPTADA", "RECHAZADA", "CANCELADA"),
            ReservationStatus.entries.map { it.name },
        )
        assertEquals(listOf("PASAJERO", "CONDUCTOR", "AMBOS"), PreferredMode.entries.map { it.name })
        assertEquals(listOf("MANANA", "TARDE", "NOCHE"), Shift.entries.map { it.name })
    }
}

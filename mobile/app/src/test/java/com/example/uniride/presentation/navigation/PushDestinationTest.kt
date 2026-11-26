package com.example.uniride.presentation.navigation

import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.model.TripId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What a tapped push opens (docs/API.md §9). */
class PushDestinationTest {

    private val tripId = "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31"

    @Test
    fun `una solicitud nueva abre el panel del conductor`() {
        assertEquals(
            PushDestination.DriverPanel(TripId(tripId)),
            PushDestination.fromExtras(type = "NUEVA_SOLICITUD", tripId = tripId),
        )
    }

    @Test
    fun `una solicitud aceptada abre mi viaje`() {
        assertEquals(
            PushDestination.MyTrip(TripId(tripId)),
            PushDestination.fromExtras(type = "SOLICITUD_ACEPTADA", tripId = tripId),
        )
    }

    @Test
    fun `una solicitud rechazada abre mi viaje`() {
        assertEquals(
            PushDestination.MyTrip(TripId(tripId)),
            PushDestination.fromExtras(type = "SOLICITUD_RECHAZADA", tripId = tripId),
        )
    }

    @Test
    fun `el panel es del modo conductor y mi viaje del modo pasajero`() {
        assertEquals(AppMode.CONDUCTOR, PushDestination.DriverPanel(TripId(tripId)).mode)
        assertEquals(AppMode.PASAJERO, PushDestination.MyTrip(TripId(tripId)).mode)
    }

    @Test
    fun `un intent sin datos de push no abre nada`() {
        assertNull(PushDestination.fromExtras(type = null, tripId = null))
    }

    @Test
    fun `un tipo desconocido no abre nada`() {
        assertNull(PushDestination.fromExtras(type = "CALIFICAR_VIAJE", tripId = tripId))
    }

    @Test
    fun `una push sin viaje no abre nada`() {
        assertNull(PushDestination.fromExtras(type = "NUEVA_SOLICITUD", tripId = null))
    }

    @Test
    fun `un id de viaje que no es un UUID no abre nada`() {
        assertNull(PushDestination.fromExtras(type = "NUEVA_SOLICITUD", tripId = "../trips"))
    }
}

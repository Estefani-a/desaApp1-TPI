package com.example.uniride.data.push

import com.example.uniride.domain.model.NotificationType
import com.example.uniride.domain.model.TripId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PushPayloadTest {

    @Test
    fun `parsea payload valido de docs API 9 con todos los campos`() {
        val payload = PushPayload.fromData(
            mapOf(
                PushPayload.KEY_TYPE to "SOLICITUD_ACEPTADA",
                PushPayload.KEY_NOTIFICATION_ID to "e1f4b7a0-2c5d-4e8f-9b3a-6d0c1e4f7a29",
                PushPayload.KEY_TRIP_ID to "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31",
                PushPayload.KEY_TITLE to "¡Tenés lugar!",
                PushPayload.KEY_BODY to "Lucía F. aceptó tu solicitud para el viaje de las 22:15.",
            ),
        )

        assertEquals(NotificationType.SOLICITUD_ACEPTADA, payload?.type)
        assertEquals(TripId("0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31"), payload?.tripId)
        assertEquals("e1f4b7a0-2c5d-4e8f-9b3a-6d0c1e4f7a29", payload?.notificationId)
        assertEquals("¡Tenés lugar!", payload?.title)
        assertEquals("Lucía F. aceptó tu solicitud para el viaje de las 22:15.", payload?.body)
    }

    @Test
    fun `normaliza tripId UUID en mayusculas a minusculas`() {
        val payload = PushPayload.fromData(
            mapOf(
                PushPayload.KEY_TYPE to "NUEVA_SOLICITUD",
                PushPayload.KEY_TRIP_ID to "0B7C4F0E-5D1A-4F5A-8A52-7C9E5E0D2B31",
            ),
        )

        assertEquals(TripId("0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31"), payload?.tripId)
    }

    @Test
    fun `ignora tipo desconocido`() {
        val payload = PushPayload.fromData(
            mapOf(
                PushPayload.KEY_TYPE to "CALIFICAR_VIAJE",
                PushPayload.KEY_TRIP_ID to "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31",
            ),
        )

        assertNull(payload)
    }

    @Test
    fun `ignora payload sin tipo`() {
        val payload = PushPayload.fromData(
            mapOf(PushPayload.KEY_TRIP_ID to "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31"),
        )

        assertNull(payload)
    }

    @Test
    fun `ignora tripId que no es un UUID valido`() {
        val payload = PushPayload.fromData(
            mapOf(
                PushPayload.KEY_TYPE to "SOLICITUD_RECHAZADA",
                PushPayload.KEY_TRIP_ID to "not-a-uuid",
            ),
        )

        assertNull(payload)
    }

    @Test
    fun `convierte titulo y cuerpo en blanco a null para usar fallback local`() {
        val payload = PushPayload.fromData(
            mapOf(
                PushPayload.KEY_TYPE to "SOLICITUD_RECHAZADA",
                PushPayload.KEY_TRIP_ID to "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31",
                PushPayload.KEY_TITLE to "   ",
                PushPayload.KEY_BODY to "",
            ),
        )

        assertNotNull(payload)
        assertNull(payload?.title)
        assertNull(payload?.body)
    }

    @Test
    fun `notificationIntId distingue notificaciones diferentes y es estable para la misma`() {
        val first = requireNotNull(
            PushPayload.fromData(
                mapOf(
                    PushPayload.KEY_TYPE to "NUEVA_SOLICITUD",
                    PushPayload.KEY_NOTIFICATION_ID to "notif-1",
                    PushPayload.KEY_TRIP_ID to "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31",
                ),
            ),
        )
        val duplicate = first.copy()
        val second = first.copy(notificationId = "notif-2")

        assertEquals(first.notificationIntId, duplicate.notificationIntId)
        assertNotEquals(first.notificationIntId, second.notificationIntId)
    }
}

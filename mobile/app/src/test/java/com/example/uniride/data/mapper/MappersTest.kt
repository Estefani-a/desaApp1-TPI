package com.example.uniride.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun `un viaje con todos los datos opcionales vuelve igual despues de pasar por la entity`() {
        val trip = MapperFixtures.fullTrip()

        assertEquals(trip, trip.toEntity(isSync = true).toDomain())
    }

    @Test
    fun `un viaje sin datos opcionales vuelve igual despues de pasar por la entity`() {
        val trip = MapperFixtures.minimalTrip()

        assertEquals(trip, trip.toEntity(isSync = true).toDomain())
    }

    @Test
    fun `el viaje conserva el valor de isSync que recibe`() {
        val trip = MapperFixtures.fullTrip()

        assertFalse(trip.toEntity(isSync = false).isSync)
        assertTrue(trip.toEntity(isSync = true).isSync)
    }

    @Test
    fun `la entity del viaje guarda la sede como codigo y el telefono del conductor`() {
        val entity = MapperFixtures.fullTrip().toEntity(isSync = true)

        assertEquals("MONSERRAT", entity.campus)
        assertEquals("1155551234", entity.driverPhone)
    }

    @Test
    fun `una reserva con telefono y calificacion vuelve igual despues de pasar por la entity`() {
        val reservation = MapperFixtures.reservation(MapperFixtures.fullPassenger())

        assertEquals(reservation, reservation.toEntity(isSync = true).toDomain())
    }

    @Test
    fun `una reserva sin telefono ni calificacion vuelve igual despues de pasar por la entity`() {
        val reservation = MapperFixtures.reservation(MapperFixtures.emptyPassenger())

        assertEquals(reservation, reservation.toEntity(isSync = true).toDomain())
    }

    @Test
    fun `la reserva conserva el valor de isSync que recibe`() {
        val reservation = MapperFixtures.reservation(MapperFixtures.emptyPassenger())

        assertFalse(reservation.toEntity(isSync = false).isSync)
        assertTrue(reservation.toEntity(isSync = true).isSync)
    }

    @Test
    fun `un usuario con vehiculo y calificacion vuelve igual despues de pasar por la entity`() {
        val user = MapperFixtures.driverUser()

        assertEquals(user, user.toEntity().toDomain())
    }

    @Test
    fun `un usuario sin vehiculo ni calificacion vuelve igual despues de pasar por la entity`() {
        val user = MapperFixtures.passengerUser()

        val entity = user.toEntity()

        assertNull(entity.vehiclePlate)
        assertNull(entity.ratingAvg)
        assertEquals(0, entity.ratingCount)
        assertEquals(user, entity.toDomain())
    }

    @Test
    fun `una sede vuelve igual despues de pasar por la entity y conserva el codigo como texto`() {
        val campus = MapperFixtures.campus()

        val entity = campus.toEntity()

        assertEquals("MONSERRAT", entity.code)
        assertEquals(campus, entity.toDomain())
    }
}

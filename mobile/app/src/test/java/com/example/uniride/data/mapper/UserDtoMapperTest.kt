package com.example.uniride.data.mapper

import com.example.uniride.data.remote.dto.UserDto
import com.example.uniride.data.remote.dto.VehicleDto
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Rating
import com.example.uniride.domain.model.UserId
import com.example.uniride.domain.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDtoMapperTest {

    @Test
    fun `UserDto con vehiculo y calificacion mapea correctamente a dominio y entity`() {
        val dto = UserDto(
            id = "user-1",
            fullName = "Lucía Fernández",
            email = "lfernandez@uade.edu.ar",
            career = "Ingeniería en Informática",
            phone = "+5491155550001",
            preferredMode = "AMBOS",
            ratingAvg = 4.8,
            ratingCount = 12,
            vehicle = VehicleDto(
                brand = "Volkswagen",
                model = "Gol Trend",
                color = "Gris",
                plate = "AB123CD",
            ),
        )

        val user = dto.toDomain()

        assertEquals(UserId("user-1"), user.id)
        assertEquals("lfernandez@uade.edu.ar", user.email)
        assertEquals(PreferredMode.AMBOS, user.profile.preferredMode)
        assertEquals(Rating(4.8, 12), user.rating)
        assertTrue(user.canDrive)
        assertEquals("AB123CD", user.profile.vehicle?.plate)
        assertEquals(user, dto.toEntity().toDomain())
    }

    @Test
    fun `UserDto sin vehiculo ni calificacion deja canDrive en false y rating en null`() {
        val dto = UserDto(
            id = "user-2",
            fullName = "Martín Gómez",
            email = "mgomez@uade.edu.ar",
            career = "Licenciatura en Gestión de Tecnología",
            phone = "+5491155550002",
            preferredMode = "PASAJERO",
            ratingAvg = null,
            ratingCount = 0,
            vehicle = null,
        )

        val user = dto.toDomain()

        assertFalse(user.canDrive)
        assertNull(user.profile.vehicle)
        assertNull(user.rating)
    }

    @Test
    fun `Profile mapea a RegisterRequest y UpdateProfileRequest conservando el vehiculo`() {
        val profile = Profile(
            fullName = "Alvaro Herrera",
            career = "Ingeniería en Informática",
            phone = "+5491155559999",
            preferredMode = PreferredMode.CONDUCTOR,
            vehicle = Vehicle("Toyota", "Corolla", "Azul", "AE789JK"),
        )

        val register = profile.toRegisterRequest("aherrera@uade.edu.ar", "Secreta123")
        val update = profile.toUpdateProfileRequest()

        assertEquals("aherrera@uade.edu.ar", register.email)
        assertEquals("Secreta123", register.password)
        assertEquals("CONDUCTOR", register.preferredMode)
        assertEquals("AE789JK", register.vehicle?.plate)
        assertEquals("CONDUCTOR", update.preferredMode)
        assertEquals("Toyota", update.vehicle?.brand)
    }
}

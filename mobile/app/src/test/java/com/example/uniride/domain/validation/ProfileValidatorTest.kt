package com.example.uniride.domain.validation

import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileValidatorTest {

    @Test
    fun `perfil completo sin vehiculo pasa la validacion`() {
        val profile = Profile(
            fullName = "Lucía Fernández",
            career = "Ingeniería en Informática",
            phone = "+5491155550000",
            preferredMode = PreferredMode.PASAJERO,
            vehicle = null,
        )

        assertEquals(ProfileValidationResult.Valid, ProfileValidator.validate(profile))
    }

    @Test
    fun `perfil con campos obligatorios vacios devuelve todos los errores`() {
        val profile = Profile(
            fullName = "  ",
            career = "",
            phone = "",
            preferredMode = PreferredMode.PASAJERO,
            vehicle = null,
        )

        assertEquals(
            ProfileValidationResult.Invalid(
                setOf(
                    ProfileValidationError.FULL_NAME_BLANK,
                    ProfileValidationError.CAREER_BLANK,
                    ProfileValidationError.PHONE_BLANK,
                ),
            ),
            ProfileValidator.validate(profile),
        )
    }

    @Test
    fun `vehiculo incompleto marca los campos faltantes y normalize limpia espacios y patente`() {
        val incomplete = Profile(
            fullName = "Lucía",
            career = "Informática",
            phone = "+5491155550000",
            preferredMode = PreferredMode.CONDUCTOR,
            vehicle = Vehicle(brand = "VW", model = "", color = "Gris", plate = " "),
        )

        assertEquals(
            ProfileValidationResult.Invalid(
                setOf(
                    ProfileValidationError.VEHICLE_MODEL_BLANK,
                    ProfileValidationError.VEHICLE_PLATE_BLANK,
                ),
            ),
            ProfileValidator.validate(incomplete),
        )

        val normalized = ProfileValidator.normalize(
            Profile(
                fullName = "  Lucía Fernández ",
                career = " Informática ",
                phone = " +5491155550000 ",
                preferredMode = PreferredMode.AMBOS,
                vehicle = Vehicle(" VW ", " Gol ", " Gris ", " ab123cd "),
            ),
        )
        assertEquals("Lucía Fernández", normalized.fullName)
        assertEquals("AB123CD", normalized.vehicle?.plate)
    }
}

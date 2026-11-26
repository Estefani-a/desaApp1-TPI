package com.example.uniride.domain.validation

import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidatorTest {

    private fun validPassengerProfile() = Profile(
        fullName = "Lucía Fernández",
        career = "Ingeniería en Informática",
        phone = "+5491155550001",
        preferredMode = PreferredMode.PASAJERO,
        vehicle = null,
    )

    @Test
    fun `acepta un perfil de pasajero completo sin vehiculo`() {
        assertEquals(ProfileValidationResult.Valid, ProfileValidator.validate(validPassengerProfile()))
    }

    @Test
    fun `acepta modo AMBOS sin vehiculo para operar como pasajero`() {
        val profile = validPassengerProfile().copy(preferredMode = PreferredMode.AMBOS)

        assertEquals(ProfileValidationResult.Valid, ProfileValidator.validate(profile))
    }

    @Test
    fun `exige vehiculo cuando el modo habitual es CONDUCTOR`() {
        val profile = validPassengerProfile().copy(preferredMode = PreferredMode.CONDUCTOR, vehicle = null)

        val result = ProfileValidator.validate(profile)

        assertTrue(result is ProfileValidationResult.Invalid)
        assertEquals(
            setOf(ProfileValidationError.VEHICLE_REQUIRED),
            (result as ProfileValidationResult.Invalid).errors,
        )
    }

    @Test
    fun `rechaza campos personales obligatorios en blanco`() {
        val profile = Profile(
            fullName = "   ",
            career = "",
            phone = " ",
            preferredMode = PreferredMode.PASAJERO,
            vehicle = null,
        )

        val result = ProfileValidator.validate(profile)

        assertTrue(result is ProfileValidationResult.Invalid)
        assertEquals(
            setOf(
                ProfileValidationError.FULL_NAME_BLANK,
                ProfileValidationError.CAREER_BLANK,
                ProfileValidationError.PHONE_BLANK,
            ),
            (result as ProfileValidationResult.Invalid).errors,
        )
    }

    @Test
    fun `rechaza telefono sin digitos validos`() {
        val profile = validPassengerProfile().copy(phone = "hola")

        val result = ProfileValidator.validate(profile)

        assertTrue(result is ProfileValidationResult.Invalid)
        assertEquals(
            setOf(ProfileValidationError.PHONE_INVALID),
            (result as ProfileValidationResult.Invalid).errors,
        )
    }

    @Test
    fun `rechaza vehiculo con campos obligatorios en blanco`() {
        val profile = validPassengerProfile().copy(
            preferredMode = PreferredMode.CONDUCTOR,
            vehicle = Vehicle(brand = " ", model = "", color = " ", plate = ""),
        )

        val result = ProfileValidator.validate(profile)

        assertTrue(result is ProfileValidationResult.Invalid)
        assertEquals(
            setOf(
                ProfileValidationError.VEHICLE_BRAND_BLANK,
                ProfileValidationError.VEHICLE_MODEL_BLANK,
                ProfileValidationError.VEHICLE_COLOR_BLANK,
                ProfileValidationError.VEHICLE_PLATE_BLANK,
            ),
            (result as ProfileValidationResult.Invalid).errors,
        )
    }

    @Test
    fun `rechaza patente con formato invalido`() {
        val withSpaces = validPassengerProfile().copy(
            preferredMode = PreferredMode.CONDUCTOR,
            vehicle = Vehicle("Volkswagen", "Gol Trend", "Gris", "AB 123-CD"),
        )
        val tooShort = validPassengerProfile().copy(
            preferredMode = PreferredMode.CONDUCTOR,
            vehicle = Vehicle("Volkswagen", "Gol Trend", "Gris", "x"),
        )

        val spacesResult = ProfileValidator.validate(withSpaces) as ProfileValidationResult.Invalid
        val shortResult = ProfileValidator.validate(tooShort) as ProfileValidationResult.Invalid

        assertEquals(setOf(ProfileValidationError.VEHICLE_PLATE_INVALID), spacesResult.errors)
        assertEquals(setOf(ProfileValidationError.VEHICLE_PLATE_INVALID), shortResult.errors)
    }

    @Test
    fun `normaliza textos con trim y patente en mayusculas`() {
        val raw = Profile(
            fullName = "  Lucía Fernández  ",
            career = "  Ingeniería en Informática ",
            phone = " +5491155550001 ",
            preferredMode = PreferredMode.CONDUCTOR,
            vehicle = Vehicle(
                brand = " Volkswagen ",
                model = " Gol Trend ",
                color = " Gris ",
                plate = " ab123cd ",
            ),
        )

        val normalized = ProfileValidator.normalize(raw)

        assertEquals("Lucía Fernández", normalized.fullName)
        assertEquals("Ingeniería en Informática", normalized.career)
        assertEquals("+5491155550001", normalized.phone)
        assertEquals("Volkswagen", normalized.vehicle?.brand)
        assertEquals("Gol Trend", normalized.vehicle?.model)
        assertEquals("Gris", normalized.vehicle?.color)
        assertEquals("AB123CD", normalized.vehicle?.plate)
    }
}

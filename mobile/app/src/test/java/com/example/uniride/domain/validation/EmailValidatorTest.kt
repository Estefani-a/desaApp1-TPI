package com.example.uniride.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailValidatorTest {

    @Test
    fun `correo institucional valido pasa la validacion`() {
        val result = EmailValidator.validate("lfernandez@uade.edu.ar")

        assertEquals(EmailValidationResult.Valid, result)
    }

    @Test
    fun `correo institucional con mayusculas y espacios laterales pasa la validacion`() {
        val result = EmailValidator.validate("  L.Fernandez_1@UADE.EDU.AR  ")

        assertEquals(EmailValidationResult.Valid, result)
    }

    @Test
    fun `normalize recorta espacios y convierte a minusculas`() {
        val normalized = EmailValidator.normalize("  Alvaro.Herrera@UADE.EDU.AR ")

        assertEquals("alvaro.herrera@uade.edu.ar", normalized)
    }

    @Test
    fun `correo vacio devuelve error BLANK`() {
        val result = EmailValidator.validate("")

        assertEquals(EmailValidationResult.Invalid(EmailValidationError.BLANK), result)
    }

    @Test
    fun `correo con solo espacios devuelve error BLANK`() {
        val result = EmailValidator.validate("   ")

        assertEquals(EmailValidationResult.Invalid(EmailValidationError.BLANK), result)
    }

    @Test
    fun `correo sin arroba devuelve error INVALID_FORMAT`() {
        val result = EmailValidator.validate("lfernandez.uade.edu.ar")

        assertEquals(EmailValidationResult.Invalid(EmailValidationError.INVALID_FORMAT), result)
    }

    @Test
    fun `correo sin parte local devuelve error INVALID_FORMAT`() {
        val result = EmailValidator.validate("@uade.edu.ar")

        assertEquals(EmailValidationResult.Invalid(EmailValidationError.INVALID_FORMAT), result)
    }

    @Test
    fun `correo con puntos consecutivos devuelve error INVALID_FORMAT`() {
        val result = EmailValidator.validate("lucia..fernandez@uade.edu.ar")

        assertEquals(EmailValidationResult.Invalid(EmailValidationError.INVALID_FORMAT), result)
    }

    @Test
    fun `correo con espacios internos devuelve error INVALID_FORMAT`() {
        val result = EmailValidator.validate("lucia fernandez@uade.edu.ar")

        assertEquals(EmailValidationResult.Invalid(EmailValidationError.INVALID_FORMAT), result)
    }

    @Test
    fun `correo de dominio externo devuelve error INVALID_DOMAIN`() {
        val result = EmailValidator.validate("lfernandez@gmail.com")

        assertEquals(EmailValidationResult.Invalid(EmailValidationError.INVALID_DOMAIN), result)
    }

    @Test
    fun `correo con subdominio de uade devuelve error INVALID_DOMAIN`() {
        val result = EmailValidator.validate("lfernandez@alumnos.uade.edu.ar")

        assertEquals(EmailValidationResult.Invalid(EmailValidationError.INVALID_DOMAIN), result)
    }

    @Test
    fun `correo con dominio parecido a uade devuelve error INVALID_DOMAIN`() {
        val result = EmailValidator.validate("lfernandez@fakeuade.edu.ar")

        assertEquals(EmailValidationResult.Invalid(EmailValidationError.INVALID_DOMAIN), result)
    }
}

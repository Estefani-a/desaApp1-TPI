package com.example.uniride.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {

    @Test
    fun `contrasena con al menos 8 caracteres letra y numero es valida`() {
        val result = PasswordPolicy.validate("Secreta1")

        assertEquals(PasswordValidationResult.Valid, result)
    }

    @Test
    fun `contrasena larga con simbolos letra y numero es valida`() {
        val result = PasswordPolicy.validate("UniRide_2026!")

        assertEquals(PasswordValidationResult.Valid, result)
    }

    @Test
    fun `contrasena vacia devuelve error BLANK`() {
        val result = PasswordPolicy.validate("")

        assertEquals(
            PasswordValidationResult.Invalid(setOf(PasswordValidationError.BLANK)),
            result,
        )
    }

    @Test
    fun `contrasena con solo espacios devuelve error BLANK`() {
        val result = PasswordPolicy.validate("        ")

        assertEquals(
            PasswordValidationResult.Invalid(setOf(PasswordValidationError.BLANK)),
            result,
        )
    }

    @Test
    fun `contrasena con menos de 8 caracteres devuelve error TOO_SHORT`() {
        val result = PasswordPolicy.validate("Abc1234")

        assertEquals(
            PasswordValidationResult.Invalid(setOf(PasswordValidationError.TOO_SHORT)),
            result,
        )
    }

    @Test
    fun `contrasena de mas de 72 bytes ASCII devuelve error TOO_LONG`() {
        val result = PasswordPolicy.validate("a".repeat(72) + "1")

        assertEquals(
            PasswordValidationResult.Invalid(setOf(PasswordValidationError.TOO_LONG)),
            result,
        )
    }

    @Test
    fun `contrasena multibyte que supera 72 bytes UTF-8 devuelve error TOO_LONG`() {
        val result = PasswordPolicy.validate("ñ".repeat(36) + "1")

        assertEquals(
            PasswordValidationResult.Invalid(setOf(PasswordValidationError.TOO_LONG)),
            result,
        )
    }

    @Test
    fun `contrasena sin letras devuelve error MISSING_LETTER`() {
        val result = PasswordPolicy.validate("12345678")

        assertEquals(
            PasswordValidationResult.Invalid(setOf(PasswordValidationError.MISSING_LETTER)),
            result,
        )
    }

    @Test
    fun `contrasena sin numeros devuelve error MISSING_DIGIT`() {
        val result = PasswordPolicy.validate("SoloLetras")

        assertEquals(
            PasswordValidationResult.Invalid(setOf(PasswordValidationError.MISSING_DIGIT)),
            result,
        )
    }

    @Test
    fun `contrasena que incumple varias reglas devuelve todos los errores y primaryError consistente`() {
        val result = PasswordPolicy.validate("abc")

        assertTrue(result is PasswordValidationResult.Invalid)
        val invalid = result as PasswordValidationResult.Invalid
        assertEquals(
            setOf(PasswordValidationError.TOO_SHORT, PasswordValidationError.MISSING_DIGIT),
            invalid.errors,
        )
        assertEquals(PasswordValidationError.TOO_SHORT, invalid.primaryError)
    }
}

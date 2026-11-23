package com.example.uniride.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorCodeTest {

    @Test
    fun `codigo de error conocido se mapea al enum correspondiente`() {
        assertEquals(ApiErrorCode.NO_SEATS_AVAILABLE, ApiErrorCode.fromRaw("NO_SEATS_AVAILABLE"))
        assertEquals(ApiErrorCode.INVALID_EMAIL_DOMAIN, ApiErrorCode.fromRaw("INVALID_EMAIL_DOMAIN"))
        assertEquals(ApiErrorCode.ACCOUNT_NOT_ACTIVATED, ApiErrorCode.fromRaw("ACCOUNT_NOT_ACTIVATED"))
    }

    @Test
    fun `codigo con minusculas y espacios laterales se normaliza correctamente`() {
        assertEquals(ApiErrorCode.EMAIL_ALREADY_REGISTERED, ApiErrorCode.fromRaw("  email_already_registered "))
    }

    @Test
    fun `codigo desconocido nulo o vacio devuelve UNKNOWN`() {
        assertEquals(ApiErrorCode.UNKNOWN, ApiErrorCode.fromRaw("UNSUPPORTED_FUTURE_CODE"))
        assertEquals(ApiErrorCode.UNKNOWN, ApiErrorCode.fromRaw(""))
        assertEquals(ApiErrorCode.UNKNOWN, ApiErrorCode.fromRaw(null))
    }

    @Test
    fun `ApiErrorDto expone su errorCode tipado`() {
        val dto = ApiErrorDto(
            code = "VALIDATION_ERROR",
            message = "Hay campos con errores.",
            fields = mapOf("totalSeats" to "Debe estar entre 1 y 4."),
        )

        assertEquals(ApiErrorCode.VALIDATION_ERROR, dto.errorCode)
    }
}

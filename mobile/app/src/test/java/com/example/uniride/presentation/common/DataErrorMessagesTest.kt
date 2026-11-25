package com.example.uniride.presentation.common

import com.example.uniride.R
import com.example.uniride.core.result.DataError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataErrorMessagesTest {

    @Test
    fun `cada DataError tiene su propio mensaje`() {
        val messages = DataError.entries.map { it.messageRes() }
        assertEquals(DataError.entries.size, messages.toSet().size)
    }

    @Test
    fun `sin conexion muestra el mensaje de red`() {
        assertEquals(R.string.common_error_no_connection, DataError.NO_CONNECTION.messageRes())
    }

    @Test
    fun `una sesion vencida no ofrece reintentar`() {
        assertFalse(DataError.UNAUTHORIZED.toContentError().canRetry)
    }

    @Test
    fun `el resto de los errores se puede reintentar`() {
        DataError.entries.filter { it != DataError.UNAUTHORIZED }.forEach {
            assertTrue("$it debería permitir reintentar", it.toContentError().canRetry)
        }
    }
}

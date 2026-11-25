package com.example.uniride.core.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultExtensionsTest {

    private val success: AppResult<Int> = AppResult.Success(2)
    private val failure: AppResult<Int> = AppResult.Failure(DataError.NO_CONNECTION)

    @Test
    fun `map transforma el dato de un Success`() {
        assertEquals(AppResult.Success("2"), success.map { it.toString() })
    }

    @Test
    fun `map deja intacto un Failure y no ejecuta la transformacion`() {
        var called = false

        val result = failure.map {
            called = true
            it + 1
        }

        assertEquals(AppResult.Failure(DataError.NO_CONNECTION), result)
        assertFalse(called)
    }

    @Test
    fun `map permite descartar el dato devolviendo Unit`() {
        assertEquals(AppResult.Success(Unit), success.map { })
    }

    @Test
    fun `onSuccess ejecuta la accion con el dato y devuelve el mismo resultado`() {
        var received: Int? = null

        val result = success.onSuccess { received = it }

        assertEquals(2, received)
        assertSame(success, result)
    }

    @Test
    fun `onSuccess no ejecuta la accion en un Failure`() {
        var called = false

        val result = failure.onSuccess { called = true }

        assertFalse(called)
        assertSame(failure, result)
    }

    @Test
    fun `onFailure ejecuta la accion con el error y devuelve el mismo resultado`() {
        var received: DataError? = null

        val result = failure.onFailure { received = it }

        assertEquals(DataError.NO_CONNECTION, received)
        assertSame(failure, result)
    }

    @Test
    fun `onFailure no ejecuta la accion en un Success`() {
        var called = false

        val result = success.onFailure { called = true }

        assertFalse(called)
        assertSame(success, result)
    }

    @Test
    fun `getOrNull devuelve el dato en un Success`() {
        assertEquals(2, success.getOrNull())
    }

    @Test
    fun `getOrNull devuelve null en un Failure`() {
        assertNull(failure.getOrNull())
    }

    @Test
    fun `fold aplica la rama de exito en un Success`() {
        val text = success.fold(onSuccess = { "ok $it" }, onFailure = { "error $it" })

        assertEquals("ok 2", text)
    }

    @Test
    fun `fold aplica la rama de error en un Failure`() {
        val text = failure.fold(onSuccess = { "ok $it" }, onFailure = { "error $it" })

        assertEquals("error NO_CONNECTION", text)
    }

    @Test
    fun `se pueden encadenar onSuccess y map como en el repositorio de ejemplo`() {
        val saved = mutableListOf<Int>()

        val result = success.onSuccess { saved += it }.map { }

        assertEquals(listOf(2), saved)
        assertTrue(result is AppResult.Success)
    }
}

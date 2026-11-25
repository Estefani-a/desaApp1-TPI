package com.example.uniride.data.remote

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.remote.dto.ApiErrorDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SafeApiCallTest {

    private fun apiError(code: Int) = ApiException(code, ApiErrorDto(code = "X", message = "x"))

    @Test
    fun `una respuesta correcta es Success`() = runTest {
        assertEquals(AppResult.Success(1), safeApiCall { 1 })
    }

    @Test
    fun `los codigos HTTP se traducen como dice CONVENCIONES`() = runTest {
        val expected = mapOf(
            400 to DataError.VALIDATION,
            401 to DataError.UNAUTHORIZED,
            403 to DataError.UNKNOWN,
            404 to DataError.NOT_FOUND,
            409 to DataError.CONFLICT,
            500 to DataError.SERVER,
            503 to DataError.SERVER,
        )
        expected.forEach { (http, error) ->
            assertEquals("HTTP $http", AppResult.Failure(error), safeApiCall<Unit> { throw apiError(http) })
        }
    }

    @Test
    fun `sin red es NO_CONNECTION y un timeout es TIMEOUT`() = runTest {
        assertEquals(AppResult.Failure(DataError.NO_CONNECTION), safeApiCall<Unit> { throw IOException() })
        assertEquals(AppResult.Failure(DataError.TIMEOUT), safeApiCall<Unit> { throw SocketTimeoutException() })
        assertEquals(AppResult.Failure(DataError.NO_CONNECTION), safeApiCall<Unit> { throw UnknownHostException("offline") })
    }

    @Test
    fun `una respuesta que no se puede leer es UNKNOWN`() = runTest {
        assertEquals(AppResult.Failure(DataError.UNKNOWN), safeApiCall<Unit> { throw IllegalArgumentException() })
    }

    @Test
    fun `la cancelacion de la coroutine no se traga`() = runTest {
        try {
            safeApiCall<Unit> { throw CancellationException("cancelada") }
            fail("Tenía que relanzar la cancelación")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
    }
}

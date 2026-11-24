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

    @Test
    fun `llamada exitosa devuelve AppResult Success`() = runTest {
        val result = safeApiCall { "ok" }

        assertEquals(AppResult.Success("ok"), result)
    }

    @Test
    fun `mapea codigos HTTP de ApiException al DataError correspondiente`() = runTest {
        assertEquals(
            AppResult.Failure(DataError.VALIDATION),
            safeApiCall<Unit> { throw ApiException(400, ApiErrorDto("VALIDATION_ERROR", "err")) },
        )
        assertEquals(
            AppResult.Failure(DataError.UNAUTHORIZED),
            safeApiCall<Unit> { throw ApiException(401, ApiErrorDto("UNAUTHENTICATED", "err")) },
        )
        assertEquals(
            AppResult.Failure(DataError.UNKNOWN),
            safeApiCall<Unit> { throw ApiException(403, ApiErrorDto("FORBIDDEN", "err")) },
        )
        assertEquals(
            AppResult.Failure(DataError.NOT_FOUND),
            safeApiCall<Unit> { throw ApiException(404, ApiErrorDto("NOT_FOUND", "err")) },
        )
        assertEquals(
            AppResult.Failure(DataError.CONFLICT),
            safeApiCall<Unit> { throw ApiException(409, ApiErrorDto("NO_SEATS_AVAILABLE", "err")) },
        )
        assertEquals(
            AppResult.Failure(DataError.SERVER),
            safeApiCall<Unit> { throw ApiException(500, ApiErrorDto("INTERNAL_ERROR", "err")) },
        )
    }

    @Test
    fun `mapea errores de red a TIMEOUT y NO_CONNECTION`() = runTest {
        assertEquals(
            AppResult.Failure(DataError.TIMEOUT),
            safeApiCall<Unit> { throw SocketTimeoutException("timeout") },
        )
        assertEquals(
            AppResult.Failure(DataError.NO_CONNECTION),
            safeApiCall<Unit> { throw UnknownHostException("offline") },
        )
        assertEquals(
            AppResult.Failure(DataError.NO_CONNECTION),
            safeApiCall<Unit> { throw IOException("network error") },
        )
    }

    @Test
    fun `nunca atrapa CancellationException y la relanza`() = runTest {
        try {
            safeApiCall<Unit> { throw CancellationException("cancelado") }
            fail("Se esperaba que CancellationException se relanzara")
        } catch (_: CancellationException) {
            // Expected
        }
    }
}

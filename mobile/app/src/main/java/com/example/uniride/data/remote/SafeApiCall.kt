package com.example.uniride.data.remote

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.data.remote.dto.ApiErrorCode
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

internal data class ApiCallResult<out T>(
    val result: AppResult<T>,
    val errorCode: ApiErrorCode? = null,
)

/**
 * Runs [safeApiCall] while preserving the backend [ApiErrorCode] when an [ApiException] is thrown,
 * so repositories can map domain-specific error codes without duplicating exception handling.
 */
internal suspend fun <T> safeApiCallWithCode(block: suspend () -> T): ApiCallResult<T> {
    var code: ApiErrorCode? = null
    val result = safeApiCall {
        try {
            block()
        } catch (e: ApiException) {
            code = e.apiError.errorCode
            throw e
        }
    }
    return ApiCallResult(result = result, errorCode = code)
}

/**
 * The only place where a failed call becomes a [DataError] (CONVENCIONES §6.3). It works the
 * same with Retrofit and with the `Fake*RemoteDataSource`, which throw [ApiException].
 * [CancellationException] is rethrown, so cancelling a coroutine keeps working.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        AppResult.Failure(httpCodeToDataError(e.httpCode))
    } catch (e: HttpException) {
        AppResult.Failure(httpCodeToDataError(e.code()))
    } catch (e: SocketTimeoutException) {
        AppResult.Failure(DataError.TIMEOUT)
    } catch (e: IOException) {
        AppResult.Failure(DataError.NO_CONNECTION)
    } catch (e: Exception) {
        // An unexpected answer (for example, a field the mapper cannot read) is not a crash
        AppResult.Failure(DataError.UNKNOWN)
    }

/** Table of CONVENCIONES §10.4. */
internal fun httpCodeToDataError(code: Int): DataError = when (code) {
    400, 422 -> DataError.VALIDATION
    401 -> DataError.UNAUTHORIZED
    404 -> DataError.NOT_FOUND
    409 -> DataError.CONFLICT
    in 500..599 -> DataError.SERVER
    else -> DataError.UNKNOWN
}

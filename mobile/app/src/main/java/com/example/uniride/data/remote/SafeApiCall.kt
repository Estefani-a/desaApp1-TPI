package com.example.uniride.data.remote

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Single translation boundary from remote exceptions / HTTP status codes to [AppResult]
 * (CONVENCIONES §6.3 & §10.4). Always rethrows [CancellationException].
 */
suspend fun <T> safeApiCall(call: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(call())
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: ApiException) {
        AppResult.Failure(mapHttpStatusToDataError(ex.httpCode))
    } catch (ex: HttpException) {
        AppResult.Failure(mapHttpStatusToDataError(ex.code()))
    } catch (_: SocketTimeoutException) {
        AppResult.Failure(DataError.TIMEOUT)
    } catch (_: IOException) {
        AppResult.Failure(DataError.NO_CONNECTION)
    } catch (_: Exception) {
        AppResult.Failure(DataError.UNKNOWN)
    }

internal fun mapHttpStatusToDataError(httpCode: Int): DataError = when (httpCode) {
    400, 422 -> DataError.VALIDATION
    401 -> DataError.UNAUTHORIZED
    404 -> DataError.NOT_FOUND
    409 -> DataError.CONFLICT
    in 500..599 -> DataError.SERVER
    else -> DataError.UNKNOWN
}

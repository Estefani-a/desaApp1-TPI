package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.ApiException
import com.example.uniride.data.remote.dto.ApiErrorCode
import com.example.uniride.data.remote.dto.ApiErrorDto
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translates Retrofit [HttpException] responses into the shared [ApiException] so that
 * real and fake remote data sources report backend errors through a single type.
 */
@Singleton
class RetrofitErrorMapper @Inject constructor(
    moshi: Moshi,
) {
    private val errorAdapter = moshi.adapter(ApiErrorDto::class.java)

    suspend fun <T> execute(call: suspend () -> T): T =
        try {
            call()
        } catch (ex: HttpException) {
            throw toApiException(ex)
        }

    fun toApiException(ex: HttpException): ApiException {
        val rawBody = runCatching { ex.response()?.errorBody()?.string() }.getOrNull()
        val parsed = rawBody?.takeIf { it.isNotBlank() }?.let { json ->
            runCatching { errorAdapter.fromJson(json) }.getOrNull()
        }
        val apiError = parsed ?: ApiErrorDto(
            code = ApiErrorCode.UNKNOWN.name,
            message = ex.message().orEmpty(),
            fields = null,
        )
        return ApiException(httpCode = ex.code(), apiError = apiError)
    }
}

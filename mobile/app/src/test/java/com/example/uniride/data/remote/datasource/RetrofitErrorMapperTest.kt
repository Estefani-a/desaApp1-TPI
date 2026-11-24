package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.dto.ApiErrorCode
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RetrofitErrorMapperTest {

    private val mapper = RetrofitErrorMapper(Moshi.Builder().build())

    @Test
    fun `parsea cuerpo JSON de error del backend en ApiException`() {
        val json = """
            {
              "code": "VALIDATION_ERROR",
              "message": "Hay campos con errores.",
              "fields": { "totalSeats": "Debe estar entre 1 y 4." }
            }
        """.trimIndent()
        val httpException = HttpException(
            Response.error<Any>(400, json.toResponseBody("application/json".toMediaType())),
        )

        val apiException = mapper.toApiException(httpException)

        assertEquals(400, apiException.httpCode)
        assertEquals(ApiErrorCode.VALIDATION_ERROR, apiException.apiError.errorCode)
        assertEquals("Debe estar entre 1 y 4.", apiException.apiError.fields?.get("totalSeats"))
    }

    @Test
    fun `cuerpo no JSON devuelve ApiException con codigo UNKNOWN`() {
        val httpException = HttpException(
            Response.error<Any>(502, "Bad Gateway".toResponseBody("text/plain".toMediaType())),
        )

        val apiException = mapper.toApiException(httpException)

        assertEquals(502, apiException.httpCode)
        assertEquals(ApiErrorCode.UNKNOWN, apiException.apiError.errorCode)
    }
}

package com.example.uniride.data.remote.interceptor

import com.example.uniride.data.datastore.SessionTokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {

    @Test
    fun `adjunta header Bearer en rutas protegidas cuando hay token`() {
        val store = FakeSessionTokenStore(initialToken = "jwt-abc-123")
        val captured = execute(
            interceptor = AuthInterceptor(store),
            url = "http://10.0.2.2:8080/api/v1/trips",
            responseCode = 200,
        )

        assertEquals("Bearer jwt-abc-123", captured.header("Authorization"))
        assertEquals(0, store.unauthorizedCalls)
    }

    @Test
    fun `no adjunta header Authorization en rutas publicas de auth`() {
        val store = FakeSessionTokenStore(initialToken = "jwt-abc-123")
        val captured = execute(
            interceptor = AuthInterceptor(store),
            url = "http://10.0.2.2:8080/api/v1/auth/login",
            responseCode = 401,
        )

        assertNull(captured.header("Authorization"))
        assertEquals(0, store.unauthorizedCalls)
    }

    @Test
    fun `notifica sesion vencida cuando una ruta protegida responde 401`() {
        val store = FakeSessionTokenStore(initialToken = "jwt-expirado")
        execute(
            interceptor = AuthInterceptor(store),
            url = "http://10.0.2.2:8080/api/v1/users/me",
            responseCode = 401,
        )

        assertEquals(1, store.unauthorizedCalls)
        assertNull(store.currentToken())
    }

    @Test
    fun `no adjunta header ni notifica 401 cuando no hay token guardado`() {
        val store = FakeSessionTokenStore(initialToken = null)
        val captured = execute(
            interceptor = AuthInterceptor(store),
            url = "http://10.0.2.2:8080/api/v1/users/me",
            responseCode = 401,
        )

        assertNull(captured.header("Authorization"))
        assertEquals(0, store.unauthorizedCalls)
    }

    private fun execute(
        interceptor: AuthInterceptor,
        url: String,
        responseCode: Int,
    ): Request {
        var capturedRequest: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(responseCode)
                    .message("Test")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        client.newCall(Request.Builder().url(url).build()).execute().close()
        return checkNotNull(capturedRequest)
    }

    private class FakeSessionTokenStore(initialToken: String?) : SessionTokenStore {
        private val tokenFlow = MutableStateFlow(initialToken)
        private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var unauthorizedCalls = 0

        override val hasToken: Flow<Boolean> = tokenFlow.map { !it.isNullOrBlank() }
        override val unauthorizedEvents: SharedFlow<Unit> = _unauthorized.asSharedFlow()
        override fun currentToken(): String? = tokenFlow.value
        override suspend fun readToken(): String? = tokenFlow.value
        override suspend fun saveToken(token: String) {
            tokenFlow.value = token
        }
        override suspend fun clearToken() {
            tokenFlow.value = null
        }
        override fun notifyUnauthorized() {
            unauthorizedCalls++
            tokenFlow.value = null
            _unauthorized.tryEmit(Unit)
        }
    }
}

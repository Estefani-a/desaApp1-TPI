package com.example.uniride.data.remote.interceptor

import com.example.uniride.data.datastore.SessionTokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the Bearer JWT from [SessionTokenStore] and triggers session invalidation
 * when an authenticated request receives HTTP 401 (CONVENCIONES §6.4).
 */
class AuthInterceptor(
    private val tokenStore: SessionTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenStore.currentToken()
        val isPublicAuthRoute = original.url.encodedPath.contains("/auth/")

        val request = if (!token.isNullOrBlank() && !isPublicAuthRoute) {
            original.newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
                .build()
        } else {
            original
        }

        val response = chain.proceed(request)

        if (response.code == HTTP_UNAUTHORIZED && !isPublicAuthRoute && !token.isNullOrBlank()) {
            tokenStore.notifyUnauthorized()
        }

        return response
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val HTTP_UNAUTHORIZED = 401
    }
}

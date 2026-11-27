package com.example.uniride.data.push

import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.dto.DeviceTokenRequest
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.di.ApplicationScope
import com.example.uniride.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the backend's `fcmToken` in sync (`PUT /users/me/device-token`, docs/API.md §4.8):
 * - Whenever a session becomes active (app launch with saved session, login, or email activation),
 *   fetches the current FCM token and uploads it best-effort.
 * - Whenever Firebase rotates the token ([onNewToken]), uploads it if there is an active session.
 *
 * Logout removal (`DELETE /users/me/device-token`) is already handled by `SessionRepositoryImpl`.
 * The token is never logged.
 */
@Singleton
class PushTokenSyncManager @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val remote: AuthRemoteDataSource,
    private val tokenProvider: FcmTokenProvider,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        appScope.launch {
            sessionRepository.observeIsLoggedIn()
                .distinctUntilChanged()
                .collect { isLoggedIn ->
                    if (isLoggedIn) {
                        syncCurrentToken()
                    }
                }
        }
    }

    /** Fetches the current FCM token and uploads it best-effort. */
    suspend fun syncCurrentToken() {
        val token = tokenProvider.currentToken()?.trim()?.takeIf { it.isNotEmpty() } ?: return
        uploadBestEffort(token)
    }

    /** Called by [UniRideMessagingService.onNewToken] when Firebase rotates the device token. */
    suspend fun onNewToken(token: String) {
        val cleanToken = token.trim().takeIf { it.isNotEmpty() } ?: return
        if (!sessionRepository.observeIsLoggedIn().first()) return
        uploadBestEffort(cleanToken)
    }

    private suspend fun uploadBestEffort(token: String) {
        safeApiCall { remote.updateDeviceToken(DeviceTokenRequest(fcmToken = token)) }
    }
}

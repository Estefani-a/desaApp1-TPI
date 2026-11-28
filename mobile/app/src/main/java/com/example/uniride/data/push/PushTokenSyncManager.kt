package com.example.uniride.data.push

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.di.ApplicationScope
import com.example.uniride.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of a background device-token sync attempt for [DeviceTokenSyncWorker]. */
enum class TokenSyncOutcome {
    DONE,
    RETRY,
}

/**
 * Keeps the backend's `fcmToken` in sync (`PUT /users/me/device-token`, docs/API.md §4.6):
 * - Whenever a session becomes active (app launch with saved session, login, or email activation),
 *   fetches the current FCM token and uploads it via [SessionRepository.registerDeviceToken].
 * - Whenever Firebase rotates the token ([onNewToken]), uploads it if there is an active session.
 * - On transient failures (offline, timeout, 5xx), enqueues a network-constrained retry via
 *   [DeviceTokenSyncScheduler] so the server does not keep a stale token from a previous user.
 *
 * Logout removal (`DELETE /users/me/device-token`) and notification tray cleanup live in
 * `SessionRepositoryImpl`. The token is never logged.
 */
@Singleton
class PushTokenSyncManager @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val tokenProvider: FcmTokenProvider,
    private val retryScheduler: DeviceTokenSyncScheduler,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    fun start() {
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

    /** Called by [UniRideMessagingService.onNewToken] when Firebase rotates the device token. */
    suspend fun onNewToken(token: String) {
        val cleanToken = token.trim().takeIf { it.isNotEmpty() } ?: return
        if (!sessionRepository.observeIsLoggedIn().first()) return
        uploadOrScheduleRetry(cleanToken)
    }

    /** Called by [DeviceTokenSyncWorker] once network connectivity returns. */
    suspend fun retryTokenSync(): TokenSyncOutcome {
        if (!sessionRepository.observeIsLoggedIn().first()) return TokenSyncOutcome.DONE
        val token = tokenProvider.currentToken() ?: return TokenSyncOutcome.DONE
        return when (val result = sessionRepository.registerDeviceToken(token)) {
            is AppResult.Success -> TokenSyncOutcome.DONE
            is AppResult.Failure -> if (result.error.isTransient()) {
                TokenSyncOutcome.RETRY
            } else {
                TokenSyncOutcome.DONE
            }
        }
    }

    private suspend fun syncCurrentToken() {
        val token = tokenProvider.currentToken() ?: return
        uploadOrScheduleRetry(token)
    }

    private suspend fun uploadOrScheduleRetry(token: String) {
        val result = sessionRepository.registerDeviceToken(token)
        if (result is AppResult.Failure && result.error.isTransient()) {
            retryScheduler.scheduleRetry()
        }
    }

    private fun DataError.isTransient(): Boolean = when (this) {
        DataError.NO_CONNECTION,
        DataError.TIMEOUT,
        DataError.SERVER,
        DataError.UNKNOWN,
        -> true
        DataError.UNAUTHORIZED,
        DataError.NOT_FOUND,
        DataError.CONFLICT,
        DataError.VALIDATION,
        -> false
    }
}

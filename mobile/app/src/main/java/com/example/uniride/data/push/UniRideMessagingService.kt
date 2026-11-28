package com.example.uniride.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * FCM entry point for RF04 data-only push notifications (docs/API.md §9).
 *
 * Delegates all business logic to [PushMessageHandler] and [PushTokenSyncManager] within a bounded
 * timeout so Android does not reclaim the service before Room is refreshed. Synchronous callbacks
 * of [FirebaseMessagingService] already run off the main thread (CONVENCIONES §8 exception).
 */
@AndroidEntryPoint
class UniRideMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushMessageHandler: PushMessageHandler

    @Inject
    lateinit var pushTokenSyncManager: PushTokenSyncManager

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data.isEmpty()) return
        runBlocking {
            withTimeoutOrNull(SERVICE_TIMEOUT_MS) {
                pushMessageHandler.handleMessage(message.data)
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION") // Backend API.md §4.6 uses FCM registration tokens
    override fun onNewToken(token: String) {
        runBlocking {
            withTimeoutOrNull(SERVICE_TIMEOUT_MS) {
                pushTokenSyncManager.onNewToken(token)
            }
        }
    }

    private companion object {
        const val SERVICE_TIMEOUT_MS = 8_000L
    }
}

package com.example.uniride.data.push

import com.example.uniride.core.dispatcher.DispatchersProvider
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
 * timeout so Android does not reclaim the service before Room is refreshed.
 */
@AndroidEntryPoint
class UniRideMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushMessageHandler: PushMessageHandler

    @Inject
    lateinit var pushTokenSyncManager: PushTokenSyncManager

    @Inject
    lateinit var dispatchers: DispatchersProvider

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data.isEmpty()) return
        runBlocking(dispatchers.io) {
            withTimeoutOrNull(SERVICE_TIMEOUT_MS) {
                pushMessageHandler.handleMessage(message.data)
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION") // Backend API.md §4.8 uses FCM registration tokens
    override fun onNewToken(token: String) {
        runBlocking(dispatchers.io) {
            withTimeoutOrNull(SERVICE_TIMEOUT_MS) {
                pushTokenSyncManager.onNewToken(token)
            }
        }
    }

    private companion object {
        const val SERVICE_TIMEOUT_MS = 8_000L
    }
}

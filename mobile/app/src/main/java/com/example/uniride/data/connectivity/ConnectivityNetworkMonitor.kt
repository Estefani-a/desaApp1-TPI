package com.example.uniride.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.uniride.domain.repository.NetworkMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Android implementation of [NetworkMonitor] backed by [ConnectivityManager].
 * Emits the current connectivity status immediately and updates reactively on network changes.
 */
class ConnectivityNetworkMonitor(
    context: Context,
) : NetworkMonitor {

    private val appContext = context.applicationContext

    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(connectivityManager.isCurrentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(networkCapabilities.hasValidatedInternet())
            }

            override fun onLost(network: Network) {
                val activeNetwork = connectivityManager.activeNetwork
                val stillOnline = activeNetwork != null &&
                    activeNetwork != network &&
                    connectivityManager.getNetworkCapabilities(activeNetwork).hasValidatedInternet()
                trySend(stillOnline)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        // Emit initial status right away so offline startups (e.g. airplane mode) are reflected immediately.
        trySend(connectivityManager.isCurrentlyOnline())
        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    private fun ConnectivityManager.isCurrentlyOnline(): Boolean =
        getNetworkCapabilities(activeNetwork).hasValidatedInternet()

    private fun NetworkCapabilities?.hasValidatedInternet(): Boolean =
        this != null &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

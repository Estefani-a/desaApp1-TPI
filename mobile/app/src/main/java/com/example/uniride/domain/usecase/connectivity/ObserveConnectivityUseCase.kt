package com.example.uniride.domain.usecase.connectivity

import com.example.uniride.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the current connectivity state so presentation screens can render
 * the offline banner consistently across the app.
 */
class ObserveConnectivityUseCase @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) {
    operator fun invoke(): Flow<Boolean> = networkMonitor.isOnline
}

package com.example.uniride.fake

import com.example.uniride.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared in-memory fake of [NetworkMonitor] for unit tests (UseCases and ViewModels).
 */
class FakeNetworkMonitor(
    initialOnline: Boolean = true,
) : NetworkMonitor {

    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: Flow<Boolean> = _isOnline.asStateFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}

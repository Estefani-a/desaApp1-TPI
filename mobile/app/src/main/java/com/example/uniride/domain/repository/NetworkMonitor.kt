package com.example.uniride.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Exposes device internet connectivity as a reactive stream (true = online, false = offline).
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

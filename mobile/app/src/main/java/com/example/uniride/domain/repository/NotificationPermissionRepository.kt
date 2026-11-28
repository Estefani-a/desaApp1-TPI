package com.example.uniride.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Tracks whether the contextual notification permission prompt (RNF08) was already shown so the
 * app asks once in context and never nags the user again.
 */
interface NotificationPermissionRepository {

    fun observeHasPrompted(): Flow<Boolean>

    suspend fun markPrompted()
}

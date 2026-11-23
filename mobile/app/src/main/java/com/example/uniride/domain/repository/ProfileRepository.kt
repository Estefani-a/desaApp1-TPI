package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.User
import kotlinx.coroutines.flow.Flow

/** The logged-in user's own data (RF01). */
interface ProfileRepository {

    /** The user as last known, also offline. `null` without session. */
    fun observeCurrentUser(): Flow<User?>

    /** Brings the latest version of the user from the server. */
    suspend fun refresh(): AppResult<Unit>

    suspend fun update(profile: Profile): AppResult<User>
}

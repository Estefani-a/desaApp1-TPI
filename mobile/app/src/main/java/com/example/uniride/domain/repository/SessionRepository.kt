package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Whether the user is logged in, and logging in and out (RF01). */
interface SessionRepository {

    /** `true` while there is a session. Decides where the app starts. */
    fun observeIsLoggedIn(): Flow<Boolean>

    suspend fun login(email: String, password: String): AppResult<User>

    /** Ends the session and forgets the user's data on this device. Also used when it expires. */
    suspend fun logout()
}

package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Outcome of signing in with institutional email and password (RF01, docs/API.md §4.3). */
sealed interface LoginOutcome {
    data class Success(val user: User) : LoginOutcome
    data object InvalidCredentials : LoginOutcome
    data object AccountNotActivated : LoginOutcome
    data class Failed(val error: DataError) : LoginOutcome
}

/** Whether the user is logged in, and logging in and out (RF01). */
interface SessionRepository {

    /** `true` while there is a session. Decides where the app starts. */
    fun observeIsLoggedIn(): Flow<Boolean>

    suspend fun login(email: String, password: String): LoginOutcome

    /** Registers the current FCM device token for push notifications (docs/API.md §4.6). */
    suspend fun registerDeviceToken(fcmToken: String): AppResult<Unit>

    /** Ends the session and forgets the user's data on this device. Also used when it expires. */
    suspend fun logout()
}

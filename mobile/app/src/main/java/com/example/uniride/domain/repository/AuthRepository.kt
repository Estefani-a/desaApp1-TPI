package com.example.uniride.domain.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.ProfileUpdate
import com.example.uniride.domain.model.Registration
import com.example.uniride.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Session and profile (RF01, docs/API.md §4). The token lives only in `SessionTokenStore`;
 * this interface never exposes it.
 */
interface AuthRepository {

    /** `true` while there is a stored session. Drives the start destination of the app. */
    fun observeIsLoggedIn(): Flow<Boolean>

    /** Profile saved in Room, or `null` without session. */
    fun observeCurrentUser(): Flow<User?>

    /** Creates the account in `PENDIENTE` and sends the activation email. */
    suspend fun register(registration: Registration): AppResult<Unit>

    /** Activates the account with the token of the email link and starts the session. */
    suspend fun activateAccount(token: String): AppResult<User>

    suspend fun resendActivation(email: String): AppResult<Unit>

    suspend fun login(email: String, password: String): AppResult<User>

    /** Always succeeds if the server answered, whether the email exists or not (RF01). */
    suspend fun requestPasswordReset(email: String): AppResult<Unit>

    suspend fun confirmPasswordReset(token: String, newPassword: String): AppResult<Unit>

    /** Downloads `GET /users/me` into Room. */
    suspend fun refreshProfile(): AppResult<Unit>

    suspend fun updateProfile(update: ProfileUpdate): AppResult<User>

    /** Deletes the token and the local data (`clearAllTables()`), also after a 401. */
    suspend fun logout()
}

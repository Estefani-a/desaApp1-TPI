package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.data.datastore.SessionTokenStore
import com.example.uniride.data.local.dao.UserDao
import com.example.uniride.data.mapper.toDomain
import com.example.uniride.data.mapper.toEntity
import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.dto.ApiErrorCode
import com.example.uniride.data.remote.dto.LoginRequest
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.data.remote.safeApiCallWithCode
import com.example.uniride.di.ApplicationScope
import com.example.uniride.domain.repository.LoginOutcome
import com.example.uniride.domain.repository.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session lifecycle repository (RF01, CONVENCIONES §6.4).
 *
 * Listens globally in [appScope] to [SessionTokenStore.unauthorizedEvents] so any HTTP 401
 * (including background `SyncWorker` runs) clears the token, wipes Room (`clearAllTables()`),
 * and clears user preferences.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val tokenStore: SessionTokenStore,
    private val userDao: UserDao,
    private val localSessionCleaner: LocalSessionCleaner,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : SessionRepository {

    private val cleanupMutex = Mutex()

    init {
        appScope.launch {
            tokenStore.unauthorizedEvents.collect {
                clearLocalSession()
            }
        }
    }

    override fun observeIsLoggedIn(): Flow<Boolean> =
        tokenStore.hasToken.distinctUntilChanged()

    override suspend fun login(email: String, password: String): LoginOutcome {
        val call = safeApiCallWithCode {
            val response = remote.login(LoginRequest(email = email, password = password))
            val user = response.user.toDomain()
            localSessionCleaner.clearAll()
            userDao.upsert(user.toEntity())
            tokenStore.saveToken(response.accessToken)
            user
        }
        return when (val result = call.result) {
            is AppResult.Success -> LoginOutcome.Success(result.data)
            is AppResult.Failure -> when (call.errorCode) {
                ApiErrorCode.INVALID_CREDENTIALS -> LoginOutcome.InvalidCredentials
                ApiErrorCode.ACCOUNT_NOT_ACTIVATED -> LoginOutcome.AccountNotActivated
                else -> LoginOutcome.Failed(result.error)
            }
        }
    }

    override suspend fun logout() {
        // Best-effort remote device-token cleanup while the JWT is still attached.
        // Logout must always succeed locally even when offline or when the token already expired.
        safeApiCall { remote.deleteDeviceToken() }
        clearLocalSession()
    }

    private suspend fun clearLocalSession() = cleanupMutex.withLock {
        try {
            tokenStore.clearToken()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep going so Room and preferences are still wiped.
        }
        try {
            localSessionCleaner.clearAll()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Never crash the application-wide 401 collector.
        }
    }
}

package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.data.datastore.SessionTokenStore
import com.example.uniride.data.local.dao.UserDao
import com.example.uniride.data.mapper.toDomain
import com.example.uniride.data.mapper.toEntity
import com.example.uniride.data.mapper.toRegisterRequest
import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.dto.ActivateRequest
import com.example.uniride.data.remote.dto.ApiErrorCode
import com.example.uniride.data.remote.dto.ResendActivationRequest
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.data.remote.safeApiCallWithCode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.repository.AccountRepository
import com.example.uniride.domain.repository.ActivateOutcome
import com.example.uniride.domain.repository.RegisterOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Institutional registration and email activation repository (RF01).
 *
 * Before persisting an activated user, [localSessionCleaner] wipes any previous user's Room
 * tables and preferences so pending offline writes from another account can never be synced
 * under the newly activated token.
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val tokenStore: SessionTokenStore,
    private val userDao: UserDao,
    private val localSessionCleaner: LocalSessionCleaner,
) : AccountRepository {

    override suspend fun register(
        email: String,
        password: String,
        profile: Profile,
    ): RegisterOutcome {
        val call = safeApiCallWithCode {
            remote.register(profile.toRegisterRequest(email = email, password = password))
        }
        return when (val result = call.result) {
            is AppResult.Success -> RegisterOutcome.Registered
            is AppResult.Failure -> when (call.errorCode) {
                ApiErrorCode.EMAIL_ALREADY_REGISTERED -> RegisterOutcome.EmailAlreadyRegistered
                else -> RegisterOutcome.Failed(result.error)
            }
        }
    }

    override suspend fun activate(activationCode: String): ActivateOutcome {
        val call = safeApiCallWithCode {
            val response = remote.activate(ActivateRequest(token = activationCode))
            val user = response.user.toDomain()
            localSessionCleaner.clearAll()
            userDao.upsert(user.toEntity())
            tokenStore.saveToken(response.accessToken)
            user
        }
        return when (val result = call.result) {
            is AppResult.Success -> ActivateOutcome.Activated(result.data)
            is AppResult.Failure -> when (call.errorCode) {
                ApiErrorCode.INVALID_OR_EXPIRED_TOKEN,
                ApiErrorCode.NOT_FOUND,
                -> ActivateOutcome.InvalidOrExpiredCode
                else -> ActivateOutcome.Failed(result.error)
            }
        }
    }

    override suspend fun resendActivation(email: String): AppResult<Unit> = safeApiCall {
        remote.resendActivation(ResendActivationRequest(email = email))
    }
}

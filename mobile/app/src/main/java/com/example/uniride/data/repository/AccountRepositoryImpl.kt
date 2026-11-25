package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.map
import com.example.uniride.data.datastore.SessionTokenStore
import com.example.uniride.data.local.dao.UserDao
import com.example.uniride.data.mapper.toDomain
import com.example.uniride.data.mapper.toEntity
import com.example.uniride.data.mapper.toRegisterRequest
import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.dto.ActivateRequest
import com.example.uniride.data.remote.dto.ResendActivationRequest
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.User
import com.example.uniride.domain.repository.AccountRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Institutional registration and email activation repository (RF01).
 * Persists the activated user in Room before storing the encrypted JWT so observers of
 * `SessionRepository.observeIsLoggedIn()` see the user immediately upon entering the main graph.
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val tokenStore: SessionTokenStore,
    private val userDao: UserDao,
) : AccountRepository {

    override suspend fun register(
        email: String,
        password: String,
        profile: Profile,
    ): AppResult<Unit> = safeApiCall {
        remote.register(profile.toRegisterRequest(email = email, password = password))
    }.map { }

    override suspend fun activate(activationCode: String): AppResult<User> = safeApiCall {
        val response = remote.activate(ActivateRequest(token = activationCode))
        val user = response.user.toDomain()
        userDao.clear()
        userDao.upsert(user.toEntity())
        tokenStore.saveToken(response.accessToken)
        user
    }

    override suspend fun resendActivation(email: String): AppResult<Unit> = safeApiCall {
        remote.resendActivation(ResendActivationRequest(email = email))
    }
}

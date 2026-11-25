package com.example.uniride.data.repository

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.core.result.onFailure
import com.example.uniride.data.datastore.SessionTokenStore
import com.example.uniride.data.local.dao.UserDao
import com.example.uniride.data.local.entity.UserEntity
import com.example.uniride.data.mapper.toDomain
import com.example.uniride.data.mapper.toEntity
import com.example.uniride.data.mapper.toUpdateProfileRequest
import com.example.uniride.data.remote.datasource.AuthRemoteDataSource
import com.example.uniride.data.remote.safeApiCall
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.User
import com.example.uniride.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logged-in user profile repository over Room (RF01, CONVENCIONES §6.1).
 * Reads always come from [UserDao] so the profile is available offline. Any `UNAUTHORIZED`
 * response notifies [SessionTokenStore] so session cleanup runs consistently in both real
 * and fake backend modes.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val userDao: UserDao,
    private val tokenStore: SessionTokenStore,
) : ProfileRepository {

    override fun observeCurrentUser(): Flow<User?> =
        userDao.observeCurrentUser()
            .map { entity: UserEntity? -> entity?.toDomain() }
            .distinctUntilChanged()

    override suspend fun refresh(): AppResult<Unit> = safeApiCall {
        val user = remote.getProfile().toDomain()
        userDao.clear()
        userDao.upsert(user.toEntity())
    }.onFailure { error ->
        if (error == DataError.UNAUTHORIZED) {
            tokenStore.notifyUnauthorized()
        }
    }

    override suspend fun update(profile: Profile): AppResult<User> = safeApiCall {
        val user = remote.updateProfile(profile.toUpdateProfileRequest()).toDomain()
        userDao.clear()
        userDao.upsert(user.toEntity())
        user
    }.onFailure { error ->
        if (error == DataError.UNAUTHORIZED) {
            tokenStore.notifyUnauthorized()
        }
    }
}

package com.example.uniride.fake

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.model.User
import com.example.uniride.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Session that the test opens and closes by hand through [isLoggedIn]. */
class FakeSessionRepository(isLoggedIn: Boolean = false) : SessionRepository {

    val isLoggedIn = MutableStateFlow(isLoggedIn)

    override fun observeIsLoggedIn(): Flow<Boolean> = isLoggedIn

    override suspend fun login(email: String, password: String): AppResult<User> = error("No se usa en estos tests")

    override suspend fun logout() {
        isLoggedIn.value = false
    }
}

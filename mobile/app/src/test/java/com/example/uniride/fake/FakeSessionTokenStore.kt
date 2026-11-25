package com.example.uniride.fake

import com.example.uniride.data.datastore.SessionTokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map

/** In-memory [SessionTokenStore] for unit tests. */
class FakeSessionTokenStore(initialToken: String? = null) : SessionTokenStore {

    private val tokenState = MutableStateFlow(initialToken)
    private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)

    var unauthorizedNotifications: Int = 0
        private set

    override val hasToken: Flow<Boolean> = tokenState.map { !it.isNullOrBlank() }

    override val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

    override fun currentToken(): String? = tokenState.value

    override suspend fun readToken(): String? = tokenState.value

    override suspend fun saveToken(token: String) {
        tokenState.value = token
    }

    override suspend fun clearToken() {
        tokenState.value = null
    }

    override fun notifyUnauthorized() {
        unauthorizedNotifications++
        tokenState.value = null
        _unauthorizedEvents.tryEmit(Unit)
    }
}

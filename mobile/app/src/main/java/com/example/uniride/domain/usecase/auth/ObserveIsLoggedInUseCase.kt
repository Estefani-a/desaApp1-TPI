package com.example.uniride.domain.usecase.auth

import com.example.uniride.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Exposes whether there is an active session on this device (RF01). */
class ObserveIsLoggedInUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<Boolean> = sessionRepository.observeIsLoggedIn()
}

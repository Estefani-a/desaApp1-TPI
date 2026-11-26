package com.example.uniride.domain.usecase.session

import com.example.uniride.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether there is a session. Decides where the app starts and sends it to login when it ends. */
class ObserveIsLoggedInUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<Boolean> = sessionRepository.observeIsLoggedIn()
}

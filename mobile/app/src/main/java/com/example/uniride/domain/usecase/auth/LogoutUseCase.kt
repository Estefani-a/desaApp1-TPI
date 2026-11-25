package com.example.uniride.domain.usecase.auth

import com.example.uniride.domain.repository.SessionRepository
import javax.inject.Inject

/** Signs out the current student and clears local session data (RF01). */
class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() {
        sessionRepository.logout()
    }
}

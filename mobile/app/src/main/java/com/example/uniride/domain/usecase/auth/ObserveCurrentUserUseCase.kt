package com.example.uniride.domain.usecase.auth

import com.example.uniride.domain.model.User
import com.example.uniride.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes the logged-in student's profile from local storage (RF01). */
class ObserveCurrentUserUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(): Flow<User?> = profileRepository.observeCurrentUser()
}

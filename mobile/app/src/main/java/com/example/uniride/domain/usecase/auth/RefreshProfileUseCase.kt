package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.domain.repository.ProfileRepository
import javax.inject.Inject

/** Refreshes the logged-in student's profile from the backend into local storage (RF01). */
class RefreshProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = profileRepository.refresh()
}

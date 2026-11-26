package com.example.uniride.domain.usecase.profile

import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Profile of the logged-in user as last known, also offline. `null` without session. */
class ObserveCurrentProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(): Flow<Profile?> = profileRepository.observeCurrentUser().map { it?.profile }
}

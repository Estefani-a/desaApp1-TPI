package com.example.uniride.domain.usecase.auth

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.User
import com.example.uniride.domain.repository.ProfileRepository
import com.example.uniride.domain.validation.ProfileValidationResult
import com.example.uniride.domain.validation.ProfileValidator
import javax.inject.Inject

/** Updates the student's editable profile after enforcing domain validation rules (RF01). */
class UpdateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(profile: Profile): AppResult<User> {
        if (ProfileValidator.validate(profile) !is ProfileValidationResult.Valid) {
            return AppResult.Failure(DataError.VALIDATION)
        }
        return profileRepository.update(ProfileValidator.normalize(profile))
    }
}

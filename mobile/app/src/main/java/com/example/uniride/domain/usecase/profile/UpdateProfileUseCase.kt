package com.example.uniride.domain.usecase.profile

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.User
import com.example.uniride.domain.repository.ProfileRepository
import com.example.uniride.domain.validation.ProfileValidationError
import com.example.uniride.domain.validation.ProfileValidationResult
import com.example.uniride.domain.validation.ProfileValidator
import javax.inject.Inject

/** Result of updating the logged-in student's profile (RF01). */
sealed interface UpdateProfileResult {
    data class Updated(val user: User) : UpdateProfileResult
    data class Invalid(val errors: Set<ProfileValidationError>) : UpdateProfileResult
    data class Failed(val error: DataError) : UpdateProfileResult
}

/** Updates the student's editable profile after enforcing domain validation rules (RF01). */
class UpdateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(profile: Profile): UpdateProfileResult {
        val validation = ProfileValidator.validate(profile)
        if (validation is ProfileValidationResult.Invalid) {
            return UpdateProfileResult.Invalid(validation.errors)
        }
        return when (val result = profileRepository.update(ProfileValidator.normalize(profile))) {
            is AppResult.Success -> UpdateProfileResult.Updated(result.data)
            is AppResult.Failure -> UpdateProfileResult.Failed(result.error)
        }
    }
}

package com.example.uniride.domain.validation

import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle

enum class ProfileValidationError {
    FULL_NAME_BLANK,
    CAREER_BLANK,
    PHONE_BLANK,
    VEHICLE_BRAND_BLANK,
    VEHICLE_MODEL_BLANK,
    VEHICLE_COLOR_BLANK,
    VEHICLE_PLATE_BLANK,
}

sealed interface ProfileValidationResult {
    data object Valid : ProfileValidationResult
    data class Invalid(val errors: Set<ProfileValidationError>) : ProfileValidationResult {
        val primaryError: ProfileValidationError
            get() = errors.first()
    }
}

/**
 * Pure domain validator for student profile and optional vehicle fields (RF01).
 */
object ProfileValidator {

    fun validate(profile: Profile): ProfileValidationResult {
        val errors = buildSet {
            if (profile.fullName.isBlank()) add(ProfileValidationError.FULL_NAME_BLANK)
            if (profile.career.isBlank()) add(ProfileValidationError.CAREER_BLANK)
            if (profile.phone.isBlank()) add(ProfileValidationError.PHONE_BLANK)
            profile.vehicle?.let { vehicle ->
                if (vehicle.brand.isBlank()) add(ProfileValidationError.VEHICLE_BRAND_BLANK)
                if (vehicle.model.isBlank()) add(ProfileValidationError.VEHICLE_MODEL_BLANK)
                if (vehicle.color.isBlank()) add(ProfileValidationError.VEHICLE_COLOR_BLANK)
                if (vehicle.plate.isBlank()) add(ProfileValidationError.VEHICLE_PLATE_BLANK)
            }
        }
        return if (errors.isEmpty()) {
            ProfileValidationResult.Valid
        } else {
            ProfileValidationResult.Invalid(errors)
        }
    }

    fun normalize(profile: Profile): Profile = profile.copy(
        fullName = profile.fullName.trim(),
        career = profile.career.trim(),
        phone = profile.phone.trim(),
        vehicle = profile.vehicle?.let { normalizeVehicle(it) },
    )

    private fun normalizeVehicle(vehicle: Vehicle): Vehicle = Vehicle(
        brand = vehicle.brand.trim(),
        model = vehicle.model.trim(),
        color = vehicle.color.trim(),
        plate = vehicle.plate.trim().uppercase(),
    )
}

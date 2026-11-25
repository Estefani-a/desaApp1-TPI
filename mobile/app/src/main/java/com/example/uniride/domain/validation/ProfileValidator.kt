package com.example.uniride.domain.validation

import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle

enum class ProfileValidationError {
    FULL_NAME_BLANK,
    CAREER_BLANK,
    PHONE_BLANK,
    PHONE_INVALID,
    VEHICLE_REQUIRED,
    VEHICLE_BRAND_BLANK,
    VEHICLE_MODEL_BLANK,
    VEHICLE_COLOR_BLANK,
    VEHICLE_PLATE_BLANK,
    VEHICLE_PLATE_INVALID,
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

    private val PHONE_ALLOWED_REGEX = Regex("^\\+?[0-9\\s\\-()]{6,20}$")
    private val PLATE_REGEX = Regex("^[A-Z0-9]{6,8}$")
    private const val MIN_PHONE_DIGITS = 6

    fun validate(profile: Profile): ProfileValidationResult {
        val errors = buildSet {
            if (profile.fullName.isBlank()) add(ProfileValidationError.FULL_NAME_BLANK)
            if (profile.career.isBlank()) add(ProfileValidationError.CAREER_BLANK)
            val phone = profile.phone.trim()
            if (phone.isEmpty()) {
                add(ProfileValidationError.PHONE_BLANK)
            } else if (!isValidPhone(phone)) {
                add(ProfileValidationError.PHONE_INVALID)
            }

            val vehicle = profile.vehicle
            if (vehicle == null) {
                if (profile.preferredMode == PreferredMode.CONDUCTOR) {
                    add(ProfileValidationError.VEHICLE_REQUIRED)
                }
            } else {
                if (vehicle.brand.isBlank()) add(ProfileValidationError.VEHICLE_BRAND_BLANK)
                if (vehicle.model.isBlank()) add(ProfileValidationError.VEHICLE_MODEL_BLANK)
                if (vehicle.color.isBlank()) add(ProfileValidationError.VEHICLE_COLOR_BLANK)
                val plate = vehicle.plate.trim().uppercase()
                if (plate.isEmpty()) {
                    add(ProfileValidationError.VEHICLE_PLATE_BLANK)
                } else if (!PLATE_REGEX.matches(plate)) {
                    add(ProfileValidationError.VEHICLE_PLATE_INVALID)
                }
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
        vehicle = profile.vehicle?.let(::normalizeVehicle),
    )

    private fun normalizeVehicle(vehicle: Vehicle): Vehicle = vehicle.copy(
        brand = vehicle.brand.trim(),
        model = vehicle.model.trim(),
        color = vehicle.color.trim(),
        plate = vehicle.plate.trim().uppercase(),
    )

    private fun isValidPhone(phone: String): Boolean =
        PHONE_ALLOWED_REGEX.matches(phone) && phone.count { it in '0'..'9' } >= MIN_PHONE_DIGITS
}

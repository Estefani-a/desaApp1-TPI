package com.example.uniride.presentation.common

import androidx.annotation.StringRes
import com.example.uniride.R
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Vehicle
import com.example.uniride.domain.validation.ProfileValidationError

@StringRes
fun ProfileValidationError.messageRes(): Int = when (this) {
    ProfileValidationError.FULL_NAME_BLANK -> R.string.auth_error_full_name_blank
    ProfileValidationError.CAREER_BLANK -> R.string.auth_error_career_blank
    ProfileValidationError.PHONE_BLANK -> R.string.auth_error_phone_blank
    ProfileValidationError.PHONE_INVALID -> R.string.auth_error_phone_invalid
    ProfileValidationError.VEHICLE_REQUIRED -> R.string.auth_error_vehicle_required
    ProfileValidationError.VEHICLE_BRAND_BLANK -> R.string.auth_error_vehicle_brand_blank
    ProfileValidationError.VEHICLE_MODEL_BLANK -> R.string.auth_error_vehicle_model_blank
    ProfileValidationError.VEHICLE_COLOR_BLANK -> R.string.auth_error_vehicle_color_blank
    ProfileValidationError.VEHICLE_PLATE_BLANK -> R.string.auth_error_vehicle_plate_blank
    ProfileValidationError.VEHICLE_PLATE_INVALID -> R.string.auth_error_vehicle_plate_invalid
}

/**
 * Editable profile and optional vehicle form state shared between Register (A3) and Profile (D1)
 * so field normalization, [Profile] conversion and [ProfileValidationError] mapping live in one place.
 */
data class ProfileForm(
    val fullName: String = "",
    val career: String = "",
    val phone: String = "",
    val preferredMode: PreferredMode = PreferredMode.PASAJERO,
    val hasVehicle: Boolean = false,
    val vehicleBrand: String = "",
    val vehicleModel: String = "",
    val vehicleColor: String = "",
    val vehiclePlate: String = "",
    @param:StringRes val fullNameErrorRes: Int? = null,
    @param:StringRes val careerErrorRes: Int? = null,
    @param:StringRes val phoneErrorRes: Int? = null,
    @param:StringRes val vehicleRequiredErrorRes: Int? = null,
    @param:StringRes val vehicleBrandErrorRes: Int? = null,
    @param:StringRes val vehicleModelErrorRes: Int? = null,
    @param:StringRes val vehicleColorErrorRes: Int? = null,
    @param:StringRes val vehiclePlateErrorRes: Int? = null,
) {
    fun toProfile(): Profile = Profile(
        fullName = fullName,
        career = career,
        phone = phone,
        preferredMode = preferredMode,
        vehicle = if (hasVehicle) {
            Vehicle(
                brand = vehicleBrand,
                model = vehicleModel,
                color = vehicleColor,
                plate = vehiclePlate,
            )
        } else {
            null
        },
    )

    fun withPreferredMode(mode: PreferredMode): ProfileForm = copy(
        preferredMode = mode,
        hasVehicle = if (mode != PreferredMode.PASAJERO) true else hasVehicle,
        vehicleRequiredErrorRes = null,
    )

    fun withHasVehicle(enabled: Boolean): ProfileForm = copy(
        hasVehicle = enabled,
        vehicleRequiredErrorRes = null,
        vehicleBrandErrorRes = null,
        vehicleModelErrorRes = null,
        vehicleColorErrorRes = null,
        vehiclePlateErrorRes = null,
    )

    fun clearErrors(): ProfileForm = copy(
        fullNameErrorRes = null,
        careerErrorRes = null,
        phoneErrorRes = null,
        vehicleRequiredErrorRes = null,
        vehicleBrandErrorRes = null,
        vehicleModelErrorRes = null,
        vehicleColorErrorRes = null,
        vehiclePlateErrorRes = null,
    )

    fun withErrors(errors: Set<ProfileValidationError>): ProfileForm = copy(
        fullNameErrorRes = errors.firstMessageRes(ProfileValidationError.FULL_NAME_BLANK),
        careerErrorRes = errors.firstMessageRes(ProfileValidationError.CAREER_BLANK),
        phoneErrorRes = errors.firstMessageRes(
            ProfileValidationError.PHONE_BLANK,
            ProfileValidationError.PHONE_INVALID,
        ),
        vehicleRequiredErrorRes = errors.firstMessageRes(ProfileValidationError.VEHICLE_REQUIRED),
        vehicleBrandErrorRes = errors.firstMessageRes(ProfileValidationError.VEHICLE_BRAND_BLANK),
        vehicleModelErrorRes = errors.firstMessageRes(ProfileValidationError.VEHICLE_MODEL_BLANK),
        vehicleColorErrorRes = errors.firstMessageRes(ProfileValidationError.VEHICLE_COLOR_BLANK),
        vehiclePlateErrorRes = errors.firstMessageRes(
            ProfileValidationError.VEHICLE_PLATE_BLANK,
            ProfileValidationError.VEHICLE_PLATE_INVALID,
        ),
    )

    companion object {
        fun fromProfile(profile: Profile): ProfileForm = ProfileForm(
            fullName = profile.fullName,
            career = profile.career,
            phone = profile.phone,
            preferredMode = profile.preferredMode,
            hasVehicle = profile.vehicle != null,
            vehicleBrand = profile.vehicle?.brand.orEmpty(),
            vehicleModel = profile.vehicle?.model.orEmpty(),
            vehicleColor = profile.vehicle?.color.orEmpty(),
            vehiclePlate = profile.vehicle?.plate.orEmpty(),
        )
    }
}

@StringRes
private fun Set<ProfileValidationError>.firstMessageRes(vararg candidates: ProfileValidationError): Int? =
    candidates.firstOrNull { it in this }?.messageRes()

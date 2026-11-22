package com.example.uniride.domain.model

/**
 * Profile of the logged-in user (`UserDto`, docs/API.md §3.2). Other users never appear
 * with this model: they come inside [Trip] and [Reservation] with name and initial (RNF07).
 */
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val career: String,
    val phone: String,
    val preferredMode: PreferredMode,
    /** `null` while nobody rated the user. */
    val ratingAvg: Double?,
    val ratingCount: Int,
    /** `null` if the user did not load a vehicle. Required to publish a trip. */
    val vehicle: Vehicle?,
) {
    val canDrive: Boolean get() = vehicle != null
}

data class Vehicle(
    val brand: String,
    val model: String,
    val color: String,
    val plate: String,
)

/** Data of the registration form (`POST /auth/register`, docs/API.md §4.1). */
data class Registration(
    val email: String,
    val password: String,
    val fullName: String,
    val career: String,
    val phone: String,
    val preferredMode: PreferredMode,
    val vehicle: Vehicle?,
)

/** Editable fields of the profile (`PUT /users/me`). The email cannot be changed. */
data class ProfileUpdate(
    val fullName: String,
    val career: String,
    val phone: String,
    val preferredMode: PreferredMode,
    val vehicle: Vehicle?,
)

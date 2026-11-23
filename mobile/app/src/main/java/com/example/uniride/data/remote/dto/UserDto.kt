package com.example.uniride.data.remote.dto

/**
 * Vehicle information embedded in user profiles (docs/API.md §3.2).
 */
data class VehicleDto(
    val brand: String,
    val model: String,
    val color: String,
    val plate: String,
)

/**
 * Authenticated user profile returned by GET/PUT /users/me and AuthResponse (docs/API.md §3.2).
 */
data class UserDto(
    val id: String,
    val fullName: String,
    val email: String,
    val career: String,
    val phone: String,
    val preferredMode: String,
    val ratingAvg: Double? = null,
    val ratingCount: Int = 0,
    val vehicle: VehicleDto? = null,
)

/**
 * Request body for PUT /users/me (docs/API.md §4.6).
 */
data class UpdateProfileRequest(
    val fullName: String,
    val career: String,
    val phone: String,
    val preferredMode: String,
    val vehicle: VehicleDto? = null,
)

/**
 * Request body for PUT /users/me/device-token (docs/API.md §4.6).
 */
data class DeviceTokenRequest(
    val fcmToken: String,
)

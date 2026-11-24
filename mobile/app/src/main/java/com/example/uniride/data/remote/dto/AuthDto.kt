package com.example.uniride.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * Request body for POST /auth/register (docs/API.md §4.1).
 */
@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val career: String,
    val phone: String,
    val preferredMode: String,
    val vehicle: VehicleDto? = null,
)

/**
 * Request body for POST /auth/login (docs/API.md §4.3).
 */
@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
)

/**
 * Request body for POST /auth/activate (docs/API.md §4.2).
 */
@JsonClass(generateAdapter = true)
data class ActivateRequest(
    val token: String,
)

/**
 * Request body for POST /auth/resend-activation (docs/API.md §4.2).
 */
@JsonClass(generateAdapter = true)
data class ResendActivationRequest(
    val email: String,
)

/**
 * Request body for POST /auth/password-reset (docs/API.md §4.5).
 */
@JsonClass(generateAdapter = true)
data class PasswordResetRequest(
    val email: String,
)

/**
 * Request body for POST /auth/password-reset/confirm (docs/API.md §4.5).
 */
@JsonClass(generateAdapter = true)
data class ConfirmPasswordResetRequest(
    val token: String,
    val newPassword: String,
)

/**
 * Response body for POST /auth/login and POST /auth/activate (docs/API.md §4.4).
 */
@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    val accessToken: String,
    val expiresAt: String,
    val user: UserDto,
)

/**
 * Generic message response used by POST /auth/register and POST /auth/password-reset.
 */
@JsonClass(generateAdapter = true)
data class MessageResponseDto(
    val message: String,
)

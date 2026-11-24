package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.dto.ActivateRequest
import com.example.uniride.data.remote.dto.ApiErrorCode
import com.example.uniride.data.remote.dto.AuthResponseDto
import com.example.uniride.data.remote.dto.ConfirmPasswordResetRequest
import com.example.uniride.data.remote.dto.DeviceTokenRequest
import com.example.uniride.data.remote.dto.LoginRequest
import com.example.uniride.data.remote.dto.MessageResponseDto
import com.example.uniride.data.remote.dto.PasswordResetRequest
import com.example.uniride.data.remote.dto.RegisterRequest
import com.example.uniride.data.remote.dto.ResendActivationRequest
import com.example.uniride.data.remote.dto.UpdateProfileRequest
import com.example.uniride.data.remote.dto.UserDto
import com.example.uniride.domain.validation.EmailValidationError
import com.example.uniride.domain.validation.EmailValidationResult
import com.example.uniride.domain.validation.EmailValidator
import com.example.uniride.domain.validation.PasswordPolicy
import com.example.uniride.domain.validation.PasswordValidationResult
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * In-memory fake of [AuthRemoteDataSource] for local UI development and tests (A2).
 */
class FakeAuthRemoteDataSource(
    private val store: FakeBackendStore,
    private val simulatedDelayMs: Long = 0L,
) : AuthRemoteDataSource {

    override suspend fun register(request: RegisterRequest): MessageResponseDto {
        maybeDelay()
        return store.withState {
            when (val emailResult = EmailValidator.validate(request.email)) {
                EmailValidationResult.Valid -> Unit
                is EmailValidationResult.Invalid -> when (emailResult.error) {
                    EmailValidationError.INVALID_DOMAIN -> throw store.apiError(
                        httpCode = 400,
                        code = ApiErrorCode.INVALID_EMAIL_DOMAIN,
                        message = "Solo se permiten correos @uade.edu.ar.",
                    )
                    EmailValidationError.BLANK,
                    EmailValidationError.INVALID_FORMAT,
                    -> throw store.apiError(
                        httpCode = 400,
                        code = ApiErrorCode.VALIDATION_ERROR,
                        message = "Hay campos con errores.",
                        fields = mapOf("email" to "El correo electrónico no tiene un formato válido."),
                    )
                }
            }
            if (PasswordPolicy.validate(request.password) is PasswordValidationResult.Invalid) {
                throw store.apiError(
                    httpCode = 400,
                    code = ApiErrorCode.WEAK_PASSWORD,
                    message = "La contraseña debe tener al menos 8 caracteres, una letra y un número.",
                )
            }
            val email = EmailValidator.normalize(request.email)
            if (store.users.values.any { it.user.email.equals(email, ignoreCase = true) }) {
                throw store.apiError(409, ApiErrorCode.EMAIL_ALREADY_REGISTERED, "El correo ya está registrado.")
            }
            val userId = UUID.randomUUID().toString()
            val newUser = UserDto(
                id = userId,
                fullName = request.fullName.trim(),
                email = email,
                career = request.career.trim(),
                phone = request.phone.trim(),
                preferredMode = request.preferredMode,
                ratingAvg = null,
                ratingCount = 0,
                vehicle = request.vehicle,
            )
            store.users[userId] = FakeBackendStore.StoredUser(
                user = newUser,
                password = request.password,
                isActivated = false,
            )
            store.activationTokens["fake-activate-$email"] = userId
            MessageResponseDto("Te enviamos un correo para activar la cuenta.")
        }
    }

    override suspend fun activate(request: ActivateRequest): AuthResponseDto {
        maybeDelay()
        return store.withState {
            val userId = store.activationTokens.remove(request.token)
                ?: throw store.apiError(400, ApiErrorCode.INVALID_OR_EXPIRED_TOKEN, "Enlace inválido o vencido.")
            val stored = store.users[userId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Usuario no encontrado.")
            val activated = stored.copy(isActivated = true)
            store.users[userId] = activated
            store.currentUserId = userId
            AuthResponseDto(
                accessToken = "fake-jwt-$userId",
                expiresAt = store.expiresIn30DaysIso(),
                user = activated.user,
            )
        }
    }

    override suspend fun resendActivation(request: ResendActivationRequest) {
        maybeDelay()
        store.withState {
            val email = request.email.trim().lowercase()
            val stored = store.users.values.firstOrNull { it.user.email == email && !it.isActivated }
            if (stored != null) {
                store.activationTokens["fake-activate-$email"] = stored.user.id
            }
        }
    }

    override suspend fun login(request: LoginRequest): AuthResponseDto {
        maybeDelay()
        return store.withState {
            val email = request.email.trim().lowercase()
            val stored = store.users.values.firstOrNull { it.user.email == email }
            if (stored == null || stored.password != request.password) {
                throw store.apiError(401, ApiErrorCode.INVALID_CREDENTIALS, "Correo o contraseña incorrectos.")
            }
            if (!stored.isActivated) {
                throw store.apiError(401, ApiErrorCode.ACCOUNT_NOT_ACTIVATED, "La cuenta todavía no fue activada.")
            }
            store.currentUserId = stored.user.id
            AuthResponseDto(
                accessToken = "fake-jwt-${stored.user.id}",
                expiresAt = store.expiresIn30DaysIso(),
                user = stored.user,
            )
        }
    }

    override suspend fun requestPasswordReset(request: PasswordResetRequest): MessageResponseDto {
        maybeDelay()
        return store.withState {
            val email = request.email.trim().lowercase()
            val stored = store.users.values.firstOrNull { it.user.email == email }
            if (stored != null) {
                store.passwordResetTokens["fake-reset-$email"] = stored.user.id
            }
            MessageResponseDto("Si el correo está registrado, te enviamos un enlace para restablecer la contraseña.")
        }
    }

    override suspend fun confirmPasswordReset(request: ConfirmPasswordResetRequest) {
        maybeDelay()
        store.withState {
            if (PasswordPolicy.validate(request.newPassword) is PasswordValidationResult.Invalid) {
                throw store.apiError(
                    httpCode = 400,
                    code = ApiErrorCode.WEAK_PASSWORD,
                    message = "La contraseña debe tener al menos 8 caracteres, una letra y un número.",
                )
            }
            val userId = store.passwordResetTokens.remove(request.token)
                ?: throw store.apiError(400, ApiErrorCode.INVALID_OR_EXPIRED_TOKEN, "Enlace inválido o vencido.")
            val stored = store.users[userId]
                ?: throw store.apiError(404, ApiErrorCode.NOT_FOUND, "Usuario no encontrado.")
            store.users[userId] = stored.copy(password = request.newPassword)
        }
    }

    override suspend fun getProfile(): UserDto {
        maybeDelay()
        return store.withState { store.requireCurrentUser().user }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): UserDto {
        maybeDelay()
        return store.withState {
            val current = store.requireCurrentUser()
            val updated = current.user.copy(
                fullName = request.fullName.trim(),
                career = request.career.trim(),
                phone = request.phone.trim(),
                preferredMode = request.preferredMode,
                vehicle = request.vehicle,
            )
            store.users[updated.id] = current.copy(user = updated)
            updated
        }
    }

    override suspend fun updateDeviceToken(request: DeviceTokenRequest) {
        maybeDelay()
        store.withState {
            val current = store.requireCurrentUser()
            store.users[current.user.id] = current.copy(fcmToken = request.fcmToken)
        }
    }

    override suspend fun deleteDeviceToken() {
        maybeDelay()
        store.withState {
            val current = store.requireCurrentUser()
            store.users[current.user.id] = current.copy(fcmToken = null)
            store.currentUserId = null
        }
    }

    private suspend fun maybeDelay() {
        if (simulatedDelayMs > 0L) delay(simulatedDelayMs)
    }
}

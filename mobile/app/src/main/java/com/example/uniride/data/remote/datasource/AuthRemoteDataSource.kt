package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.dto.ActivateRequest
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

interface AuthRemoteDataSource {
    suspend fun register(request: RegisterRequest): MessageResponseDto
    suspend fun activate(request: ActivateRequest): AuthResponseDto
    suspend fun resendActivation(request: ResendActivationRequest)
    suspend fun login(request: LoginRequest): AuthResponseDto
    suspend fun requestPasswordReset(request: PasswordResetRequest): MessageResponseDto
    suspend fun confirmPasswordReset(request: ConfirmPasswordResetRequest)
    suspend fun getProfile(): UserDto
    suspend fun updateProfile(request: UpdateProfileRequest): UserDto
    suspend fun updateDeviceToken(request: DeviceTokenRequest)
    suspend fun deleteDeviceToken()
}

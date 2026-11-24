package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.api.AuthApi
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
import javax.inject.Inject

class RetrofitAuthRemoteDataSource @Inject constructor(
    private val api: AuthApi,
    private val errorMapper: RetrofitErrorMapper,
) : AuthRemoteDataSource {

    override suspend fun register(request: RegisterRequest): MessageResponseDto =
        errorMapper.execute { api.register(request) }

    override suspend fun activate(request: ActivateRequest): AuthResponseDto =
        errorMapper.execute { api.activate(request) }

    override suspend fun resendActivation(request: ResendActivationRequest) =
        errorMapper.execute { api.resendActivation(request) }

    override suspend fun login(request: LoginRequest): AuthResponseDto =
        errorMapper.execute { api.login(request) }

    override suspend fun requestPasswordReset(request: PasswordResetRequest): MessageResponseDto =
        errorMapper.execute { api.requestPasswordReset(request) }

    override suspend fun confirmPasswordReset(request: ConfirmPasswordResetRequest) =
        errorMapper.execute { api.confirmPasswordReset(request) }

    override suspend fun getProfile(): UserDto =
        errorMapper.execute { api.getProfile() }

    override suspend fun updateProfile(request: UpdateProfileRequest): UserDto =
        errorMapper.execute { api.updateProfile(request) }

    override suspend fun updateDeviceToken(request: DeviceTokenRequest) =
        errorMapper.execute { api.updateDeviceToken(request) }

    override suspend fun deleteDeviceToken() =
        errorMapper.execute { api.deleteDeviceToken() }
}

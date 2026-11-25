package com.example.uniride.data.remote.api

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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/** Retrofit interface for RF01 authentication and profile endpoints (docs/API.md §4). */
interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): MessageResponseDto

    @POST("auth/activate")
    suspend fun activate(@Body request: ActivateRequest): AuthResponseDto

    @POST("auth/resend-activation")
    suspend fun resendActivation(@Body request: ResendActivationRequest)

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponseDto

    @POST("auth/password-reset")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequest): MessageResponseDto

    @POST("auth/password-reset/confirm")
    suspend fun confirmPasswordReset(@Body request: ConfirmPasswordResetRequest)

    @GET("users/me")
    suspend fun getProfile(): UserDto

    @PUT("users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): UserDto

    @PUT("users/me/device-token")
    suspend fun updateDeviceToken(@Body request: DeviceTokenRequest)

    @DELETE("users/me/device-token")
    suspend fun deleteDeviceToken()
}

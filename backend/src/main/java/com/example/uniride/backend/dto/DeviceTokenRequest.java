package com.example.uniride.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for PUT /users/me/device-token (docs/API.md §4.6). */
public record DeviceTokenRequest(
        @NotBlank(message = "El token del dispositivo es obligatorio.")
        @Size(max = 255, message = "Máximo 255 caracteres.")
        String fcmToken
) {
}

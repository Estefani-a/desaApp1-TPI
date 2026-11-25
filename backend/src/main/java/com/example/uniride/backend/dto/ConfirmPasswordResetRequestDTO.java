package com.example.uniride.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for POST /auth/password-reset/confirm (docs/API.md §4.5). */
public record ConfirmPasswordResetRequestDTO(
        @NotBlank(message = "El token es obligatorio.")
        String token,

        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(max = 128, message = "Máximo 128 caracteres.")
        String newPassword
) {
}

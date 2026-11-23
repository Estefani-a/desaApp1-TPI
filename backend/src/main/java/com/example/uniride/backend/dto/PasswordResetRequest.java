package com.example.uniride.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/password-reset (docs/API.md §4.5). */
public record PasswordResetRequest(
        @NotBlank(message = "El correo es obligatorio.")
        String email
) {
}

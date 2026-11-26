package com.example.uniride.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/password-reset (docs/API.md §4.5). */
public record PasswordResetRequestDTO(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        String email
) {
}

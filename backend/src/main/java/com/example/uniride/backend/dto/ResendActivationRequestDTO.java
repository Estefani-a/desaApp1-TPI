package com.example.uniride.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/resend-activation (docs/API.md §4.2). */
public record ResendActivationRequestDTO(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        String email
) {
}

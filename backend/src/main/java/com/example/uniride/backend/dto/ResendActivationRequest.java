package com.example.uniride.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/resend-activation (docs/API.md §4.2). */
public record ResendActivationRequest(
        @NotBlank(message = "El correo es obligatorio.")
        String email
) {
}

package com.example.uniride.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/login (docs/API.md §4.3). */
public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        String password
) {
}

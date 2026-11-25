package com.example.uniride.backend.dto;

import com.example.uniride.backend.model.PreferredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body for POST /auth/register (docs/API.md §4.1). */
public record RegisterRequestDTO(
        @NotBlank(message = "El correo es obligatorio.")
        @Size(max = 120, message = "Máximo 120 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 128, message = "Máximo 128 caracteres.")
        String password,

        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 100, message = "Máximo 100 caracteres.")
        String fullName,

        @NotBlank(message = "La carrera es obligatoria.")
        @Size(max = 100, message = "Máximo 100 caracteres.")
        String career,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Size(max = 30, message = "Máximo 30 caracteres.")
        String phone,

        @NotNull(message = "El modo habitual es obligatorio.")
        PreferredMode preferredMode,

        @Valid
        VehicleDTO vehicle
) {
}

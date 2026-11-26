package com.example.uniride.backend.dto;

import com.example.uniride.backend.model.PreferredMode;
import com.example.uniride.backend.model.UserProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body for POST /auth/register (docs/API.md §4.1). */
public record RegisterRequestDTO(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 120, message = "Máximo 120 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 72, message = "Máximo 72 caracteres.")
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
    public UserProfile toProfile() {
        return new UserProfile(
                fullName,
                career,
                phone,
                preferredMode,
                vehicle != null ? vehicle.toEntity() : null
        );
    }
}

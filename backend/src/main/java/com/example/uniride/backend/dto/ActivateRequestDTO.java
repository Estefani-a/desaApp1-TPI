package com.example.uniride.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/activate (docs/API.md §4.2). */
public record ActivateRequestDTO(
        @NotBlank(message = "El token es obligatorio.")
        String token
) {
}

package com.example.uniride.backend.dto;

import java.time.Instant;

/** Response for POST /auth/login and POST /auth/activate (docs/API.md §4.4). */
public record AuthResponseDTO(
        String accessToken,
        Instant expiresAt,
        UserResponseDTO user
) {
}

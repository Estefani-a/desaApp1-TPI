package com.example.uniride.backend.dto;

/** Generic message response for POST /auth/register and POST /auth/password-reset. */
public record MessageResponse(String message) {
}

package com.example.uniride.backend.service;

import com.example.uniride.backend.model.AuthTokenType;

/**
 * Domain event published inside RF01 transactions and consumed {@code AFTER_COMMIT}
 * by {@link AuthMailService} so emails are never sent if the database commit rolls back.
 */
public record AuthEmailEvent(
        AuthTokenType type,
        String toEmail,
        String fullName,
        String rawToken
) {
}

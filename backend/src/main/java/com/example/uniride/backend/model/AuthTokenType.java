package com.example.uniride.backend.model;

import java.time.Duration;

/** Single-use email verification tokens for RF01 (docs/API.md §4.2 and §4.5). */
public enum AuthTokenType {
    ACTIVATION(Duration.ofHours(24), "24 horas"),
    PASSWORD_RESET(Duration.ofHours(1), "1 hora");

    private final Duration ttl;
    private final String humanDuration;

    AuthTokenType(Duration ttl, String humanDuration) {
        this.ttl = ttl;
        this.humanDuration = humanDuration;
    }

    public Duration ttl() {
        return ttl;
    }

    public String humanDuration() {
        return humanDuration;
    }
}

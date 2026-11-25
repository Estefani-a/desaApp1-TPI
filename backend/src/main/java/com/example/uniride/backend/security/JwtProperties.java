package com.example.uniride.backend.security;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * {@code app.jwt.*} in application.yml. The secret comes from the {@code JWT_SECRET}
 * environment variable; HS256 needs at least 32 bytes, so the app does not start with a
 * shorter one.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotNull @Size(min = 32, message = "JWT_SECRET tiene que tener al menos 32 caracteres") String secret,
        @NotNull Duration expiration
) {
}

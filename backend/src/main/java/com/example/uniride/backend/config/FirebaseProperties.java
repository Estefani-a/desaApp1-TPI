package com.example.uniride.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional path to the Firebase service-account JSON (CONVENCIONES §10.6, .env.example).
 * When blank, push delivery falls back to structured logging without breaking startup or tests.
 */
@ConfigurationProperties(prefix = "app.firebase")
public record FirebaseProperties(String credentialsPath) {

    public boolean hasCredentialsPath() {
        return credentialsPath != null && !credentialsPath.isBlank();
    }
}

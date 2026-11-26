package com.example.uniride.backend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code app.links.*} in application.yml. Used by RF01 to build activation and password-reset
 * links in emails (docs/API.md §8).
 */
@Validated
@ConfigurationProperties(prefix = "app.links")
public record AppLinkProperties(
        @NotBlank String baseUrl,
        @NotBlank String mailFrom
) {
    public String normalizedBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}

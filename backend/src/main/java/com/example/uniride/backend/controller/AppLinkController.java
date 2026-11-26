package com.example.uniride.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Intermediate HTML pages for activation and password-reset emails (docs/API.md §8).
 * Email clients block custom schemes directly, so these public endpoints render a minimal page
 * with an "Abrir UniRide" button pointing to {@code uniride://...}.
 */
@RestController
@RequestMapping("/api/v1/links")
public class AppLinkController {

    @GetMapping(value = "/activate", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> activateLink(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(invalidLinkHtml());
        }
        String deepLink = "uniride://activate?token=" + URLEncoder.encode(token.trim(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(deepLinkHtml(
                "Activar cuenta en UniRide",
                "Tocá el botón para abrir UniRide y completar la activación de tu cuenta.",
                deepLink
        ));
    }

    @GetMapping(value = "/reset-password", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> resetPasswordLink(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(invalidLinkHtml());
        }
        String deepLink = "uniride://reset-password?token=" + URLEncoder.encode(token.trim(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(deepLinkHtml(
                "Restablecer contraseña en UniRide",
                "Tocá el botón para abrir UniRide y elegir una nueva contraseña.",
                deepLink
        ));
    }

    private String deepLinkHtml(String title, String description, String deepLink) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>%s</title>
                </head>
                <body style="font-family: sans-serif; max-width: 480px; margin: 40px auto; padding: 0 16px;">
                  <h1>%s</h1>
                  <p>%s</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 20px;background:#1a56db;color:#fff;text-decoration:none;border-radius:8px;">Abrir UniRide</a></p>
                  <p style="color:#555;font-size:14px;">Si la app no se abre automáticamente, verificá que tenés instalada la aplicación UniRide en este dispositivo.</p>
                </body>
                </html>
                """.formatted(title, title, description, deepLink);
    }

    private String invalidLinkHtml() {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8" /><title>Enlace inválido · UniRide</title></head>
                <body style="font-family: sans-serif; max-width: 480px; margin: 40px auto; padding: 0 16px;">
                  <h1>Enlace inválido</h1>
                  <p>El enlace está incompleto o venció. Solicitá uno nuevo desde la app UniRide.</p>
                </body>
                </html>
                """;
    }
}

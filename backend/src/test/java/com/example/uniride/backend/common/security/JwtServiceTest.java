package com.example.uniride.backend.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "secreto-de-prueba-de-al-menos-32-caracteres!!";
    private static final Instant NOW = Instant.parse("2026-11-20T21:40:12Z");
    private static final UUID USER_ID = UUID.fromString("6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10");

    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofDays(30));

    private JwtService serviceAt(Instant instant) {
        return new JwtService(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void tokenEmitidoLlevaElIdDelUsuarioEnSub() {
        JwtService service = serviceAt(NOW);

        JwtService.IssuedToken token = service.issue(USER_ID);

        String subject = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .clock(() -> Date.from(NOW))
                .build()
                .parseSignedClaims(token.accessToken())
                .getPayload()
                .getSubject();
        assertThat(subject).isEqualTo(USER_ID.toString());
        assertThat(service.parseUserId(token.accessToken())).contains(USER_ID);
    }

    @Test
    void tokenVenceALos30Dias() {
        JwtService.IssuedToken token = serviceAt(NOW).issue(USER_ID);

        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(serviceAt(NOW.plus(Duration.ofDays(29))).parseUserId(token.accessToken())).contains(USER_ID);
        assertThat(serviceAt(NOW.plus(Duration.ofDays(31))).parseUserId(token.accessToken())).isEmpty();
    }

    @Test
    void tokenFirmadoConOtroSecretoEsRechazado() {
        JwtService other = new JwtService(
                new JwtProperties("otro-secreto-distinto-de-al-menos-32-caracteres", Duration.ofDays(30)),
                Clock.fixed(NOW, ZoneOffset.UTC));

        String foreignToken = other.issue(USER_ID).accessToken();

        assertThat(serviceAt(NOW).parseUserId(foreignToken)).isEmpty();
    }

    @Test
    void tokenMalFormadoEsRechazado() {
        assertThat(serviceAt(NOW).parseUserId("no-es-un-jwt")).isEmpty();
        assertThat(serviceAt(NOW).parseUserId("")).isEmpty();
    }
}

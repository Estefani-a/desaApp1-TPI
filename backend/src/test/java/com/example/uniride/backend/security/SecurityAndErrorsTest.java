package com.example.uniride.backend.security;

import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.config.SecurityConfig;
import com.example.uniride.backend.config.TimeConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security rules and error format of docs/API.md §1–§2, checked against a controller that
 * only exists in the tests.
 */
@WebMvcTest(controllers = SecurityAndErrorsTest.ProbeController.class)
@Import({SecurityConfig.class, TimeConfig.class, JwtService.class, JwtAuthenticationFilter.class,
        ApiErrorSecurityHandler.class, CurrentUser.class, SecurityAndErrorsTest.ProbeController.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=secreto-de-prueba-de-al-menos-32-caracteres!!",
        "app.jwt.expiration=30d",
})
class SecurityAndErrorsTest {

    private static final UUID USER_ID = UUID.fromString("6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    private String bearer() {
        return "Bearer " + jwtService.issue(USER_ID).accessToken();
    }

    @Test
    void rutaProtegidaSinTokenDevuelve401ConElFormatoDeError() throws Exception {
        mvc.perform(get("/api/v1/probe/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fields").isEmpty());
    }

    @Test
    void rutaProtegidaConTokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/probe/me").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void tokenValidoIdentificaAlUsuarioPorSub() throws Exception {
        mvc.perform(get("/api/v1/probe/me").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    @Test
    void rutasDeAuthSonPublicas() throws Exception {
        mvc.perform(get("/api/v1/auth/probe"))
                .andExpect(status().isOk());
    }

    @Test
    void campusesEsPublico() throws Exception {
        mvc.perform(get("/api/v1/campuses"))
                .andExpect(status().isOk());
    }

    @Test
    void apiExceptionUsaElCodigoYElEstadoDeErrorCode() throws Exception {
        mvc.perform(get("/api/v1/probe/no-seats").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_SEATS_AVAILABLE"))
                .andExpect(jsonPath("$.message").value("El viaje ya no tiene plazas disponibles."))
                .andExpect(jsonPath("$.fields").isEmpty());
    }

    @Test
    void cuerpoInvalidoDevuelveValidationErrorConFields() throws Exception {
        mvc.perform(put("/api/v1/probe/" + UUID.randomUUID())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meetingPoint\":\"\",\"totalSeats\":7,\"departureTime\":\"22:15\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.totalSeats").value("Debe estar entre 1 y 4."))
                .andExpect(jsonPath("$.fields.meetingPoint").exists());
    }

    @Test
    void idQueNoEsUuidDevuelveValidationError() throws Exception {
        mvc.perform(put("/api/v1/probe/no-es-uuid")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meetingPoint\":\"Salida Lima 717\",\"totalSeats\":3,\"departureTime\":\"22:15\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.id").exists());
    }

    @Test
    void jsonMalFormadoDevuelveValidationError() throws Exception {
        mvc.perform(put("/api/v1/probe/" + UUID.randomUUID())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rutaInexistenteDevuelveNotFound() throws Exception {
        mvc.perform(get("/api/v1/probe/no-existe/nada").header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void metodoNoPermitidoDevuelve405ConSuCodigo() throws Exception {
        mvc.perform(post("/api/v1/probe/me").header("Authorization", bearer()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void cuerpoQueNoEsJsonDevuelve415() throws Exception {
        mvc.perform(put("/api/v1/probe/" + UUID.randomUUID())
                        .header("Authorization", bearer())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void claveDuplicadaEnLaBaseDevuelve409() throws Exception {
        mvc.perform(get("/api/v1/probe/duplicate").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ID_CONFLICT"));
    }

    @Test
    void otraViolacionDeIntegridadSigueSiendo500() throws Exception {
        mvc.perform(get("/api/v1/probe/not-null").header("Authorization", bearer()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void errorInesperadoDevuelve500SinStackTrace() throws Exception {
        mvc.perform(get("/api/v1/probe/boom").header("Authorization", bearer()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void horasSeSerializanComoHHmmEInstantesConLaZonaDeBuenosAires() throws Exception {
        mvc.perform(put("/api/v1/probe/" + UUID.randomUUID())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meetingPoint\":\"Salida Lima 717\",\"totalSeats\":3,\"departureTime\":\"22:15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departureTime").value("22:15"))
                .andExpect(jsonPath("$.updatedAt").value("2026-11-20T21:40:12-03:00"));
    }

    @RestController
    static class ProbeController {

        private final CurrentUser currentUser;

        ProbeController(CurrentUser currentUser) {
            this.currentUser = currentUser;
        }

        @GetMapping("/api/v1/probe/me")
        Map<String, UUID> me() {
            return Map.of("userId", currentUser.id());
        }

        @GetMapping("/api/v1/auth/probe")
        Map<String, String> publicAuth() {
            return Map.of("ok", "true");
        }

        @GetMapping("/api/v1/campuses")
        Map<String, String> campuses() {
            return Map.of("ok", "true");
        }

        @GetMapping("/api/v1/probe/no-seats")
        void noSeats() {
            throw new ApiException(ErrorCode.NO_SEATS_AVAILABLE);
        }

        @GetMapping("/api/v1/probe/duplicate")
        void duplicate() {
            throw new DataIntegrityViolationException("dup", new SQLException("duplicate key", "23505"));
        }

        @GetMapping("/api/v1/probe/not-null")
        void notNull() {
            throw new DataIntegrityViolationException("null", new SQLException("null value", "23502"));
        }

        @GetMapping("/api/v1/probe/boom")
        void boom() {
            throw new IllegalStateException("detalle interno que no tiene que salir");
        }

        @PutMapping("/api/v1/probe/{id}")
        ProbeResponse put(@PathVariable UUID id, @Valid @RequestBody ProbeRequest request) {
            return new ProbeResponse(request.departureTime(), Instant.parse("2026-11-21T00:40:12.345Z"));
        }
    }

    record ProbeRequest(
            @NotBlank String meetingPoint,
            @Min(value = 1, message = "Debe estar entre 1 y 4.")
            @Max(value = 4, message = "Debe estar entre 1 y 4.") int totalSeats,
            LocalTime departureTime) {
    }

    record ProbeResponse(LocalTime departureTime, Instant updatedAt) {
    }
}

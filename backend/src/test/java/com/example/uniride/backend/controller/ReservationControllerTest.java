package com.example.uniride.backend.controller;

import com.example.uniride.backend.security.ApiErrorSecurityHandler;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.security.CurrentUser;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.security.JwtAuthenticationFilter;
import com.example.uniride.backend.security.JwtProperties;
import com.example.uniride.backend.security.JwtService;
import com.example.uniride.backend.dto.ReservationResponseDTO;
import com.example.uniride.backend.service.ReservationService;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.config.SecurityConfig;
import com.example.uniride.backend.config.TimeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import({SecurityConfig.class, TimeConfig.class, JwtService.class, JwtAuthenticationFilter.class,
        ApiErrorSecurityHandler.class, CurrentUser.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=secreto-de-prueba-de-al-menos-32-caracteres!!",
        "app.jwt.expiration=30d",
})
class ReservationControllerTest {

    private static final UUID USER_ID = UUID.fromString("9a2e5c7b-3d8f-4b1a-a6e0-1c4f7d9b2e58");
    private static final UUID TRIP_ID = UUID.fromString("0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31");
    private static final UUID RESERVATION_ID = UUID.fromString("c3d9a8e2-7f41-4e0b-b1a6-5f2e9c4d8a17");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private ReservationService reservationService;

    private String bearer() {
        return "Bearer " + jwtService.issue(USER_ID).accessToken();
    }

    private ReservationResponseDTO response(ReservationStatus status) {
        Instant at = Instant.parse("2026-11-21T00:41:03Z");
        return new ReservationResponseDTO(RESERVATION_ID, TRIP_ID, USER_ID, "Martín G.", 4.5, null, status, at, at);
    }

    private String url() {
        return "/api/v1/trips/" + TRIP_ID + "/reservations/" + RESERVATION_ID;
    }

    @Test
    void solicitarPorPrimeraVezDevuelve201ConElFormatoDelContrato() throws Exception {
        when(reservationService.requestSeat(USER_ID, TRIP_ID, RESERVATION_ID))
                .thenReturn(new ReservationService.RequestSeatResult(response(ReservationStatus.PENDIENTE), true));

        mvc.perform(put(url()).header("Authorization", bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(RESERVATION_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.passengerPhone").isEmpty())
                .andExpect(jsonPath("$.reservedAt").value("2026-11-20T21:41:03-03:00"));
    }

    @Test
    void reintentarLaSolicitudDevuelve200() throws Exception {
        when(reservationService.requestSeat(USER_ID, TRIP_ID, RESERVATION_ID))
                .thenReturn(new ReservationService.RequestSeatResult(response(ReservationStatus.PENDIENTE), false));

        mvc.perform(put(url()).header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void aceptarSinVacantesDevuelve409NoSeatsAvailable() throws Exception {
        when(reservationService.accept(USER_ID, TRIP_ID, RESERVATION_ID))
                .thenThrow(new ApiException(ErrorCode.NO_SEATS_AVAILABLE));

        mvc.perform(post(url() + "/accept").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_SEATS_AVAILABLE"));
    }

    @Test
    void verSolicitudesDeUnViajeAjenoDevuelve403() throws Exception {
        when(reservationService.listForTrip(USER_ID, TRIP_ID)).thenThrow(new ApiException(ErrorCode.FORBIDDEN));

        mvc.perform(get("/api/v1/trips/" + TRIP_ID + "/reservations").header("Authorization", bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void cancelarDevuelveLaReservaCancelada() throws Exception {
        when(reservationService.cancel(USER_ID, TRIP_ID, RESERVATION_ID)).thenReturn(response(ReservationStatus.CANCELADA));

        mvc.perform(post(url() + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    void idQueNoEsUuidDevuelve400() throws Exception {
        mvc.perform(put("/api/v1/trips/" + TRIP_ID + "/reservations/no-es-uuid").header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(reservationService);
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(put(url()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(reservationService);
    }
}

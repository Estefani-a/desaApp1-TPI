package com.example.uniride.backend.controller;

import com.example.uniride.backend.config.SecurityConfig;
import com.example.uniride.backend.config.TimeConfig;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.Direction;
import com.example.uniride.backend.model.TripStatus;
import com.example.uniride.backend.security.ApiErrorSecurityHandler;
import com.example.uniride.backend.security.CurrentUser;
import com.example.uniride.backend.security.JwtAuthenticationFilter;
import com.example.uniride.backend.security.JwtProperties;
import com.example.uniride.backend.security.JwtService;
import com.example.uniride.backend.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /trips/{tripId}}: JSON of the contract (docs/API.md §3.3) and its errors. */
@WebMvcTest(TripController.class)
@Import({SecurityConfig.class, TimeConfig.class, JwtService.class, JwtAuthenticationFilter.class,
        ApiErrorSecurityHandler.class, CurrentUser.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=secreto-de-prueba-de-al-menos-32-caracteres!!",
        "app.jwt.expiration=30d",
})
class TripControllerTest {

    private static final UUID USER_ID = UUID.fromString("9a2e5c7b-3d8f-4b1a-a6e0-1c4f7d9b2e58");
    private static final UUID TRIP_ID = UUID.fromString("0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private TripService tripService;

    private String bearer() {
        return "Bearer " + jwtService.issue(USER_ID).accessToken();
    }

    private TripResponseDTO tripWithoutContact() {
        return new TripResponseDTO(TRIP_ID, UUID.randomUUID(), "Lucía F.", 4.8, Direction.VUELTA, "MONSERRAT",
                "Salida Lima 717", "Ramos Mejía", null, LocalDate.of(2026, 11, 20), LocalTime.of(22, 15), null,
                3, 2, 3500, "Gol Trend", "Gris", null, null, TripStatus.ABIERTO, null,
                Instant.parse("2026-11-21T00:40:12Z"));
    }

    @Test
    void elDetalleTieneLosCamposDelContratoConPatenteYTelefonoEnNull() throws Exception {
        when(tripService.getDetail(USER_ID, TRIP_ID)).thenReturn(tripWithoutContact());

        mvc.perform(get("/api/v1/trips/" + TRIP_ID).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(TRIP_ID.toString()))
                .andExpect(jsonPath("$.driverName").value("Lucía F."))
                .andExpect(jsonPath("$.departureTime").value("22:15"))
                .andExpect(jsonPath("$.contributionPerSeat").value(3500))
                .andExpect(jsonPath("$.vehiclePlate").isEmpty())
                .andExpect(jsonPath("$.driverPhone").isEmpty())
                .andExpect(jsonPath("$.myReservation").isEmpty())
                .andExpect(jsonPath("$.updatedAt").value("2026-11-20T21:40:12-03:00"));
    }

    @Test
    void unViajeQueNoExisteDevuelve404() throws Exception {
        when(tripService.getDetail(USER_ID, TRIP_ID)).thenThrow(new ApiException(ErrorCode.NOT_FOUND));

        mvc.perform(get("/api/v1/trips/" + TRIP_ID).header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/trips/" + TRIP_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}

package com.example.uniride.backend.controller;

import com.example.uniride.backend.config.SecurityConfig;
import com.example.uniride.backend.config.TimeConfig;
import com.example.uniride.backend.dto.PublishTripRequestDTO;
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
import com.example.uniride.backend.service.TripPublicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripPublicationController.class)
@Import({SecurityConfig.class, TimeConfig.class, JwtService.class, JwtAuthenticationFilter.class,
        ApiErrorSecurityHandler.class, CurrentUser.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=secreto-de-prueba-de-al-menos-32-caracteres!!",
        "app.jwt.expiration=30d",
})
class TripPublicationControllerTest {

    private static final UUID USER_ID = UUID.fromString("6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10");
    private static final UUID TRIP_ID = UUID.fromString("0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31");

    private static final String BODY = """
            {"direction":"VUELTA","campus":"MONSERRAT","meetingPoint":"Salida Lima 717",
             "destinationNeighborhood":"Ramos Mejía","destinationReference":"Estación de tren",
             "departureDate":"2026-11-20","departureTime":"22:15","estimatedArrivalTime":"23:00",
             "totalSeats":3,"contributionPerSeat":3500}
            """;

    private static final PublishTripRequestDTO REQUEST = new PublishTripRequestDTO(Direction.VUELTA, "MONSERRAT",
            "Salida Lima 717", "Ramos Mejía", "Estación de tren", LocalDate.of(2026, 11, 20),
            LocalTime.of(22, 15), LocalTime.of(23, 0), 3, 3500);

    /** The example of docs/API.md §3.3, as the driver sees it. */
    private static final String TRIP_JSON = """
            {"tripId":"0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31",
             "driverId":"6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10",
             "driverName":"Lucía F.","driverRating":4.8,"direction":"VUELTA","campus":"MONSERRAT",
             "meetingPoint":"Salida Lima 717","destinationNeighborhood":"Ramos Mejía",
             "destinationReference":"Estación de tren","departureDate":"2026-11-20","departureTime":"22:15",
             "estimatedArrivalTime":"23:00","totalSeats":3,"availableSeats":3,"contributionPerSeat":3500,
             "vehicleModel":"Gol Trend","vehicleColor":"Gris","vehiclePlate":"AB123CD",
             "driverPhone":"+5491155550000","status":"ABIERTO","myReservation":null,
             "updatedAt":"2026-11-20T21:40:12-03:00"}
            """;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private TripPublicationService publicationService;

    private String bearer() {
        return "Bearer " + jwtService.issue(USER_ID).accessToken();
    }

    private static TripResponseDTO response(TripStatus status) {
        return new TripResponseDTO(TRIP_ID, USER_ID, "Lucía F.", 4.8, Direction.VUELTA, "MONSERRAT",
                "Salida Lima 717", "Ramos Mejía", "Estación de tren", LocalDate.of(2026, 11, 20),
                LocalTime.of(22, 15), LocalTime.of(23, 0), 3, 3, 3500, "Gol Trend", "Gris", "AB123CD",
                "+5491155550000", status, null, Instant.parse("2026-11-21T00:40:12Z"));
    }

    private MockHttpServletRequestBuilder publish(String body) {
        return put("/api/v1/trips/" + TRIP_ID).header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void givenPublish(boolean created) {
        when(publicationService.publish(USER_ID, TRIP_ID, REQUEST))
                .thenReturn(new TripPublicationService.PublishResult(response(TripStatus.ABIERTO), created));
    }

    @Test
    void publicarDevuelve201ConExactamenteLosCamposDeTripDto() throws Exception {
        givenPublish(true);

        mvc.perform(publish(BODY))
                .andExpect(status().isCreated())
                // Strict: no field more and no field less than docs/API.md §3.3
                .andExpect(content().json(TRIP_JSON, true));
    }

    @Test
    void reintentarLaPublicacionDevuelve200() throws Exception {
        givenPublish(false);

        mvc.perform(publish(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(TRIP_ID.toString()));
    }

    @Test
    void plazasYAporteFueraDeRangoDevuelven400ConLosCampos() throws Exception {
        String body = BODY.replace("\"totalSeats\":3", "\"totalSeats\":5")
                .replace("\"contributionPerSeat\":3500", "\"contributionPerSeat\":-1");

        mvc.perform(publish(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.totalSeats").value("Debe estar entre 1 y 4."))
                .andExpect(jsonPath("$.fields.contributionPerSeat").exists());
        verifyNoInteractions(publicationService);
    }

    @Test
    void ceroPlazasDevuelve400() throws Exception {
        mvc.perform(publish(BODY.replace("\"totalSeats\":3", "\"totalSeats\":0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.totalSeats").exists());
        verifyNoInteractions(publicationService);
    }

    @Test
    void cuerpoVacioDevuelve400ConCadaCampoObligatorio() throws Exception {
        mvc.perform(publish("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.direction").exists())
                .andExpect(jsonPath("$.fields.campus").exists())
                .andExpect(jsonPath("$.fields.meetingPoint").exists())
                .andExpect(jsonPath("$.fields.destinationNeighborhood").exists())
                .andExpect(jsonPath("$.fields.departureDate").exists())
                .andExpect(jsonPath("$.fields.departureTime").exists())
                .andExpect(jsonPath("$.fields.totalSeats").exists())
                .andExpect(jsonPath("$.fields.contributionPerSeat").exists());
        verifyNoInteractions(publicationService);
    }

    @Test
    void unaFechaMalEscritaDevuelve400() throws Exception {
        mvc.perform(publish(BODY.replace("2026-11-20", "20/11/2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(publicationService);
    }

    @Test
    void unErrorDeReglaDeNegocioDevuelveSusCampos() throws Exception {
        when(publicationService.publish(USER_ID, TRIP_ID, REQUEST)).thenThrow(
                ApiException.validation(java.util.Map.of("departureDate", "Debe ser una fecha futura.")));

        mvc.perform(publish(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Hay campos con errores."))
                .andExpect(jsonPath("$.fields.departureDate").value("Debe ser una fecha futura."));
    }

    @Test
    void elIdDeOtroUsuarioDevuelve409() throws Exception {
        when(publicationService.publish(USER_ID, TRIP_ID, REQUEST))
                .thenThrow(new ApiException(ErrorCode.ID_CONFLICT));

        mvc.perform(publish(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ID_CONFLICT"))
                .andExpect(jsonPath("$.fields").isEmpty());
    }

    @Test
    void finalizarYCancelarDevuelvenElViaje() throws Exception {
        when(publicationService.finish(USER_ID, TRIP_ID)).thenReturn(response(TripStatus.FINALIZADO));
        when(publicationService.cancel(USER_ID, TRIP_ID)).thenReturn(response(TripStatus.CANCELADO));

        mvc.perform(post("/api/v1/trips/" + TRIP_ID + "/finish").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));
        mvc.perform(post("/api/v1/trips/" + TRIP_ID + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    void finalizarUnViajeAjenoDevuelve403YUnaTransicionInvalida409() throws Exception {
        when(publicationService.finish(USER_ID, TRIP_ID)).thenThrow(new ApiException(ErrorCode.FORBIDDEN));
        when(publicationService.cancel(USER_ID, TRIP_ID))
                .thenThrow(new ApiException(ErrorCode.INVALID_STATUS_TRANSITION));

        mvc.perform(post("/api/v1/trips/" + TRIP_ID + "/finish").header("Authorization", bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(post("/api/v1/trips/" + TRIP_ID + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void misViajesDevuelveLaLista() throws Exception {
        when(publicationService.listMine(USER_ID)).thenReturn(List.of(response(TripStatus.ABIERTO)));

        mvc.perform(get("/api/v1/users/me/trips").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(TRIP_ID.toString()));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/users/me/trips"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(put("/api/v1/trips/" + TRIP_ID).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(publicationService);
    }
}

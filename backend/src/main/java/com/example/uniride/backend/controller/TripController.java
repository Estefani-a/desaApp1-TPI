package com.example.uniride.backend.controller;

import com.example.uniride.backend.dto.TripFiltersRequestDTO;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.model.Direction;
import com.example.uniride.backend.model.Shift;
import com.example.uniride.backend.security.CurrentUser;
import com.example.uniride.backend.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Trips (docs/API.md §5). Only delegates to {@link TripService}. */
@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Viajes")
public class TripController {

    private final TripService tripService;
    private final CurrentUser currentUser;

    public TripController(TripService tripService, CurrentUser currentUser) {
        this.tripService = tripService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Listado de viajes con filtros (RF03): abiertos y completos que no salieron, o los cambiados desde updatedSince")
    public List<TripResponseDTO> list(
            @Parameter(description = "Código de la sede, por ejemplo MONSERRAT")
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) Direction direction,
            @Parameter(description = "Fecha de salida, 2026-11-20")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Con date, la franja que empieza ese día (NOCHE incluye la madrugada siguiente)")
            @RequestParam(required = false) Shift shift,
            @Parameter(description = "Instante ISO-8601 con zona, 2026-11-20T21:00:00-03:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime updatedSince) {
        return tripService.list(currentUser.id(),
                new TripFiltersRequestDTO(campus, direction, date, shift, updatedSince));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Detalle del viaje (patente y teléfono solo con reserva aceptada)")
    public TripResponseDTO getDetail(@PathVariable UUID tripId) {
        return tripService.getDetail(currentUser.id(), tripId);
    }
}

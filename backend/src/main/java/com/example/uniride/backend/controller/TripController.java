package com.example.uniride.backend.controller;

import com.example.uniride.backend.security.CurrentUser;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{tripId}")
    @Operation(summary = "Detalle del viaje (patente y teléfono solo con reserva aceptada)")
    public TripResponseDTO getDetail(@PathVariable UUID tripId) {
        return tripService.getDetail(currentUser.id(), tripId);
    }
}

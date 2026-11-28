package com.example.uniride.backend.controller;

import com.example.uniride.backend.dto.PublishTripRequestDTO;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.security.CurrentUser;
import com.example.uniride.backend.service.TripPublicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Publishing and managing trips (RF02, docs/API.md §5.4 to §5.6). Only delegates to
 * {@link TripPublicationService}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Publicación de viajes (RF02)")
public class TripPublicationController {

    private final TripPublicationService publicationService;
    private final CurrentUser currentUser;

    public TripPublicationController(TripPublicationService publicationService, CurrentUser currentUser) {
        this.publicationService = publicationService;
        this.currentUser = currentUser;
    }

    @PutMapping("/trips/{tripId}")
    @Operation(summary = "Publicar un viaje (idempotente: 201 al crear, 200 en un reintento)")
    public ResponseEntity<TripResponseDTO> publish(@PathVariable UUID tripId,
                                                   @Valid @RequestBody PublishTripRequestDTO request) {
        var result = publicationService.publish(currentUser.id(), tripId, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.trip());
    }

    @PostMapping("/trips/{tripId}/finish")
    @Operation(summary = "Finalizar un viaje (solo el conductor)")
    public TripResponseDTO finish(@PathVariable UUID tripId) {
        return publicationService.finish(currentUser.id(), tripId);
    }

    @PostMapping("/trips/{tripId}/cancel")
    @Operation(summary = "Cancelar un viaje (solo el conductor): cancela sus reservas activas")
    public TripResponseDTO cancel(@PathVariable UUID tripId) {
        return publicationService.cancel(currentUser.id(), tripId);
    }

    @GetMapping("/users/me/trips")
    @Operation(summary = "Mis viajes: publicados y reservados, con los cerrados de los últimos 7 días")
    public List<TripResponseDTO> listMine() {
        return publicationService.listMine(currentUser.id());
    }
}

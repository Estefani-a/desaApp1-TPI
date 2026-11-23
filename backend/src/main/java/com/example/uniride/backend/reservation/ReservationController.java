package com.example.uniride.backend.reservation;

import com.example.uniride.backend.common.security.CurrentUser;
import com.example.uniride.backend.reservation.dto.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Reservations of a trip (docs/API.md §6). Only delegates to {@link ReservationService}. */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/reservations")
@Tag(name = "Reservas (RF04)")
public class ReservationController {

    private final ReservationService reservationService;
    private final CurrentUser currentUser;

    public ReservationController(ReservationService reservationService, CurrentUser currentUser) {
        this.reservationService = reservationService;
        this.currentUser = currentUser;
    }

    @PutMapping("/{reservationId}")
    @Operation(summary = "Solicitar un asiento (idempotente: 201 al crear, 200 en un reintento)")
    public ResponseEntity<ReservationResponse> requestSeat(@PathVariable UUID tripId,
                                                           @PathVariable UUID reservationId) {
        var result = reservationService.requestSeat(currentUser.id(), tripId, reservationId);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.reservation());
    }

    @GetMapping
    @Operation(summary = "Solicitudes del viaje (solo el conductor)")
    public List<ReservationResponse> list(@PathVariable UUID tripId) {
        return reservationService.listForTrip(currentUser.id(), tripId);
    }

    @PostMapping("/{reservationId}/accept")
    @Operation(summary = "Aceptar una solicitud (conductor): descuenta una vacante")
    public ReservationResponse accept(@PathVariable UUID tripId, @PathVariable UUID reservationId) {
        return reservationService.accept(currentUser.id(), tripId, reservationId);
    }

    @PostMapping("/{reservationId}/reject")
    @Operation(summary = "Rechazar una solicitud (conductor)")
    public ReservationResponse reject(@PathVariable UUID tripId, @PathVariable UUID reservationId) {
        return reservationService.reject(currentUser.id(), tripId, reservationId);
    }

    @PostMapping("/{reservationId}/cancel")
    @Operation(summary = "Cancelar mi reserva (pasajero): si estaba aceptada, devuelve la vacante")
    public ReservationResponse cancel(@PathVariable UUID tripId, @PathVariable UUID reservationId) {
        return reservationService.cancel(currentUser.id(), tripId, reservationId);
    }
}

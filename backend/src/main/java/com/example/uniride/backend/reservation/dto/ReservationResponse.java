package com.example.uniride.backend.reservation.dto;

import com.example.uniride.backend.reservation.Reservation;
import com.example.uniride.backend.reservation.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code ReservationDto} of docs/API.md §3.4. {@code passengerPhone} only has a value for
 * the driver of the trip once the reservation is {@code ACEPTADA} (RNF07).
 */
public record ReservationResponse(
        UUID reservationId,
        UUID tripId,
        UUID passengerId,
        String passengerName,
        Double passengerRating,
        String passengerPhone,
        ReservationStatus status,
        Instant reservedAt,
        Instant updatedAt
) {

    /** @param viewerId user that makes the request: decides whether the phone is shown. */
    public static ReservationResponse from(Reservation reservation, UUID viewerId) {
        boolean showPhone = reservation.getTrip().isDrivenBy(viewerId)
                && reservation.getStatus() == ReservationStatus.ACEPTADA;
        return new ReservationResponse(
                reservation.getId(),
                reservation.getTrip().getId(),
                reservation.getPassenger().getId(),
                reservation.getPassenger().getPublicName(),
                reservation.getPassenger().getRatingAvg(),
                showPhone ? reservation.getPassenger().getPhone() : null,
                reservation.getStatus(),
                reservation.getReservedAt(),
                reservation.getUpdatedAt());
    }
}

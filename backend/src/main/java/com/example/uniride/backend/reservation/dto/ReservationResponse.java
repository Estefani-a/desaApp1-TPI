package com.example.uniride.backend.reservation.dto;

import com.example.uniride.backend.reservation.Reservation;
import com.example.uniride.backend.reservation.ReservationStatus;
import com.example.uniride.backend.user.PublicName;

import java.time.Instant;
import java.util.UUID;

/** {@code ReservationDto} of docs/API.md §3.4. */
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

    /**
     * @param showPhone decided by {@code ContactVisibilityPolicy}; this record only copies values
     */
    public static ReservationResponse from(Reservation reservation, boolean showPhone) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getTrip().getId(),
                reservation.getPassenger().getId(),
                PublicName.of(reservation.getPassenger().getFullName()),
                reservation.getPassenger().getRatingAvg(),
                showPhone ? reservation.getPassenger().getPhone() : null,
                reservation.getStatus(),
                reservation.getReservedAt(),
                reservation.getUpdatedAt());
    }
}

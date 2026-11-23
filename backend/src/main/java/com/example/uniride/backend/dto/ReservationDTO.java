package com.example.uniride.backend.dto;

import com.example.uniride.backend.service.ContactVisibilityPolicy;
import com.example.uniride.backend.util.PublicName;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

/** {@code ReservationDto} of docs/API.md §3.4. */
public record ReservationDTO(
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
    public static ReservationDTO from(Reservation reservation, boolean showPhone) {
        return new ReservationDTO(
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

package com.example.uniride.backend.service;

import com.example.uniride.backend.model.NotificationType;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.util.PublicName;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Domain event published inside reservation transactions and delivered after commit by
 * {@link PushNotificationService}, so a rolled-back transaction never sends a push (docs/API.md §9).
 * Uses {@link PublicName} to preserve RNF07 privacy in notification copy.
 */
public record PushNotificationEvent(
        NotificationType type,
        UUID recipientUserId,
        UUID tripId,
        UUID reservationId,
        String title,
        String body
) {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static PushNotificationEvent newRequest(Reservation reservation) {
        Trip trip = reservation.getTrip();
        String passengerName = PublicName.of(reservation.getPassenger().getFullName());
        String time = trip.getSchedule().getDepartureTime().format(TIME_FORMAT);
        return new PushNotificationEvent(
                NotificationType.NUEVA_SOLICITUD,
                trip.getDriver().getId(),
                trip.getId(),
                reservation.getId(),
                "Nueva solicitud de asiento",
                "%s pidió un asiento para tu viaje de las %s.".formatted(passengerName, time)
        );
    }

    public static PushNotificationEvent requestAccepted(Reservation reservation) {
        Trip trip = reservation.getTrip();
        String driverName = PublicName.of(trip.getDriver().getFullName());
        String time = trip.getSchedule().getDepartureTime().format(TIME_FORMAT);
        return new PushNotificationEvent(
                NotificationType.SOLICITUD_ACEPTADA,
                reservation.getPassenger().getId(),
                trip.getId(),
                reservation.getId(),
                "¡Tenés lugar!",
                "%s aceptó tu solicitud para el viaje de las %s.".formatted(driverName, time)
        );
    }

    public static PushNotificationEvent requestRejected(Reservation reservation) {
        Trip trip = reservation.getTrip();
        String driverName = PublicName.of(trip.getDriver().getFullName());
        String time = trip.getSchedule().getDepartureTime().format(TIME_FORMAT);
        return new PushNotificationEvent(
                NotificationType.SOLICITUD_RECHAZADA,
                reservation.getPassenger().getId(),
                trip.getId(),
                reservation.getId(),
                "Solicitud rechazada",
                "%s rechazó tu solicitud para el viaje de las %s.".formatted(driverName, time)
        );
    }
}

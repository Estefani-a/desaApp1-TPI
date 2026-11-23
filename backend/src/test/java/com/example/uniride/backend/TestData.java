package com.example.uniride.backend;

import com.example.uniride.backend.campus.Campus;
import com.example.uniride.backend.reservation.Reservation;
import com.example.uniride.backend.reservation.ReservationStatus;
import com.example.uniride.backend.trip.Direction;
import com.example.uniride.backend.trip.Trip;
import com.example.uniride.backend.user.PreferredMode;
import com.example.uniride.backend.user.User;
import com.example.uniride.backend.user.Vehicle;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** Objects for the tests. "Now" is 2026-11-20 18:00 in Buenos Aires. */
public final class TestData {

    public static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));

    private static final Campus MONSERRAT = new Campus("MONSERRAT", "Sede Monserrat", List.of("Salida Lima 717"));

    private TestData() {
    }

    public static User driver() {
        return new User("conductora.demo@uade.edu.ar", "hash", "Lucía Fernández", "Ingeniería",
                "+5491155550000", PreferredMode.AMBOS, new Vehicle("Volkswagen", "Gol Trend", "Gris", "AB123CD"));
    }

    public static User passenger() {
        return new User("pasajero.demo@uade.edu.ar", "hash", "Martín Gómez", "Administración",
                "+5491155550001", PreferredMode.PASAJERO, null);
    }

    /** Trip of [driver] today at 22:15 with [seats] seats. */
    public static Trip trip(User driver, int seats) {
        return new Trip(UUID.randomUUID(), driver, Direction.VUELTA, MONSERRAT, "Salida Lima 717",
                "Ramos Mejía", null, LocalDate.of(2026, 11, 20), LocalTime.of(22, 15), null,
                seats, 3500, "Gol Trend", "Gris", "AB123CD");
    }

    public static Reservation reservation(Trip trip, User passenger, ReservationStatus status) {
        Reservation reservation = new Reservation(UUID.randomUUID(), trip, passenger);
        reservation.setStatus(status);
        return reservation;
    }
}

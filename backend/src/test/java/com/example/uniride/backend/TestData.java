package com.example.uniride.backend;

import com.example.uniride.backend.model.Campus;
import com.example.uniride.backend.model.Direction;
import com.example.uniride.backend.model.PreferredMode;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.model.TripRoute;
import com.example.uniride.backend.model.TripSchedule;
import com.example.uniride.backend.model.TripStatus;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.model.UserProfile;
import com.example.uniride.backend.model.Vehicle;
import org.springframework.test.util.ReflectionTestUtils;

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
    public static final Instant NOW = CLOCK.instant();
    /** A moment after the trips of {@link #trip(User, int)} left. */
    public static final Instant AFTER_DEPARTURE = Instant.parse("2026-11-21T03:00:00Z");

    public static final Campus MONSERRAT = new Campus("MONSERRAT", "Sede Monserrat", List.of("Salida Lima 717"));

    private TestData() {
    }

    public static User driver() {
        return new User("conductora.demo@uade.edu.ar", "hash", new UserProfile("Lucía Fernández", "Ingeniería",
                "+5491155550000", PreferredMode.AMBOS, new Vehicle("Volkswagen", "Gol Trend", "Gris", "AB123CD")), NOW);
    }

    public static User passenger() {
        return new User("pasajero.demo@uade.edu.ar", "hash", new UserProfile("Martín Gómez", "Administración",
                "+5491155550001", PreferredMode.PASAJERO, null), NOW);
    }

    /** Trip of [driver] on [date] at 22:15 with [seats] seats. */
    public static Trip trip(User driver, int seats, LocalDate date) {
        return new Trip(UUID.randomUUID(), driver, MONSERRAT,
                new TripRoute(Direction.VUELTA, "Salida Lima 717", "Ramos Mejía", null),
                new TripSchedule(date, LocalTime.of(22, 15), null),
                seats, 3500, NOW);
    }

    /** Trip of [driver] today at 22:15 (after "now") with [seats] seats. */
    public static Trip trip(User driver, int seats) {
        return trip(driver, seats, LocalDate.of(2026, 11, 20));
    }

    /** Leaves [trip] without seats, accepting one request per seat. */
    public static void fill(Trip trip) {
        while (trip.getAvailableSeats() > 0) {
            reservation(trip, passenger(), ReservationStatus.ACEPTADA);
        }
    }

    /** Puts [trip] in [status] directly: finishing and cancelling trips belong to RF02. */
    public static void close(Trip trip, TripStatus status) {
        ReflectionTestUtils.setField(trip, "status", status);
    }

    /** Reservation of [passenger] on [trip] taken to [status] through its transitions. */
    public static Reservation reservation(Trip trip, User passenger, ReservationStatus status) {
        Reservation reservation = new Reservation(UUID.randomUUID(), trip, passenger, NOW);
        switch (status) {
            case PENDIENTE -> { }
            case ACEPTADA -> reservation.accept(NOW);
            case RECHAZADA -> reservation.reject(NOW);
            case CANCELADA -> reservation.cancel(NOW);
        }
        return reservation;
    }
}

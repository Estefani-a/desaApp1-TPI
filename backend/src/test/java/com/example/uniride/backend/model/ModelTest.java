package com.example.uniride.backend.model;

import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.util.PublicName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static com.example.uniride.backend.TestData.AFTER_DEPARTURE;
import static com.example.uniride.backend.TestData.close;
import static com.example.uniride.backend.TestData.fill;
import static com.example.uniride.backend.TestData.NOW;
import static com.example.uniride.backend.TestData.driver;
import static com.example.uniride.backend.TestData.passenger;
import static com.example.uniride.backend.TestData.trip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Invariants of the entities: they only change through their methods. */
class ModelTest {

    private static ErrorCode errorOf(Executable action) {
        return assertThrows(ApiException.class, action).getCode();
    }

    private static Reservation pending(Trip trip) {
        return new Reservation(UUID.randomUUID(), trip, passenger(), NOW);
    }

    // --- Usuario ---

    @Test
    void cuentaNuevaQuedaPendienteHastaActivarla() {
        User user = passenger();

        assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDIENTE);

        user.activate(NOW);

        assertThat(user.isActive()).isTrue();
    }

    @Test
    void nombrePublicoEsNombreEInicialDelApellido() {
        assertThat(PublicName.of("Lucía Fernández")).isEqualTo("Lucía F.");
        assertThat(PublicName.of("  Martín  Ezequiel Gómez ")).isEqualTo("Martín G.");
        assertThat(PublicName.of("Cher")).isEqualTo("Cher");
    }

    @Test
    void sinVehiculoNoSePuedePublicar() {
        User noVehicle = new User("a@uade.edu.ar", "hash",
                new UserProfile("Ana Paz", "Derecho", "+5491100000000", PreferredMode.CONDUCTOR, null), NOW);

        assertThat(errorOf(() -> trip(noVehicle, 3))).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    // --- Viaje ---

    @Test
    void elViajeCopiaElVehiculoDelPerfil() {
        Trip trip = trip(driver(), 3);

        assertThat(trip.getVehicle().getPlate()).isEqualTo("AB123CD");
        assertThat(trip.getVehicle()).isNotSameAs(trip.getDriver().getVehicle());
    }

    @Test
    void tomarLaUltimaVacanteCompletaElViajeYLiberarlaLoReabre() {
        Trip trip = trip(driver(), 1);

        trip.takeSeat(NOW);

        assertThat(trip.getAvailableSeats()).isZero();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETO);
        assertThat(errorOf(() -> trip.takeSeat(NOW))).isEqualTo(ErrorCode.NO_SEATS_AVAILABLE);

        trip.releaseSeat(NOW);

        assertThat(trip.getAvailableSeats()).isEqualTo(1);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.ABIERTO);
    }

    @Test
    void liberarMasVacantesQueLasTotalesEsUnError() {
        Trip trip = trip(driver(), 2);

        assertThatThrownBy(() -> trip.releaseSeat(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unViajeAbiertoConVacantesAceptaSolicitudes() {
        trip(driver(), 3).requireOpenForRequests(NOW);
    }

    @Test
    void unViajeQueYaSalioNoAceptaSolicitudes() {
        assertThat(errorOf(() -> trip(driver(), 3).requireOpenForRequests(AFTER_DEPARTURE)))
                .isEqualTo(ErrorCode.TRIP_NOT_OPEN);
    }

    @Test
    void unViajeCanceladoNoAceptaSolicitudes() {
        Trip trip = trip(driver(), 3);
        close(trip, TripStatus.CANCELADO);

        assertThat(errorOf(() -> trip.requireOpenForRequests(NOW))).isEqualTo(ErrorCode.TRIP_NOT_OPEN);
    }

    @Test
    void unViajeCompletoNoTieneVacantesParaSolicitudes() {
        Trip trip = trip(driver(), 1);
        fill(trip);

        assertThat(errorOf(() -> trip.requireOpenForRequests(NOW))).isEqualTo(ErrorCode.NO_SEATS_AVAILABLE);
    }

    // --- Reserva ---

    @Test
    void aceptarTomaUnaVacanteYRepetirloNoTomaOtra() {
        Trip trip = trip(driver(), 3);
        Reservation reservation = pending(trip);

        reservation.accept(NOW);
        reservation.accept(NOW);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACEPTADA);
        assertThat(reservation.isAccepted()).isTrue();
        assertThat(trip.getAvailableSeats()).isEqualTo(2);
    }

    @Test
    void sinVacantesLaReservaSigueSiendoPendiente() {
        Trip trip = trip(driver(), 1);
        fill(trip);
        Reservation late = pending(trip);

        assertThat(errorOf(() -> late.accept(NOW))).isEqualTo(ErrorCode.NO_SEATS_AVAILABLE);
        assertThat(late.getStatus()).isEqualTo(ReservationStatus.PENDIENTE);
    }

    @Test
    void cancelarUnaAceptadaDevuelveLaVacante() {
        Trip trip = trip(driver(), 1);
        Reservation reservation = pending(trip);
        reservation.accept(NOW);

        reservation.cancel(NOW);
        reservation.cancel(NOW);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELADA);
        assertThat(trip.getAvailableSeats()).isEqualTo(1);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.ABIERTO);
    }

    @Test
    void aceptarUnaRechazadaEsUnaTransicionInvalida() {
        Reservation rejected = pending(trip(driver(), 3));
        rejected.reject(NOW);

        assertThat(errorOf(() -> rejected.accept(NOW))).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void cancelarUnaRechazadaEsUnaTransicionInvalida() {
        Reservation rejected = pending(trip(driver(), 3));
        rejected.reject(NOW);

        assertThat(errorOf(() -> rejected.cancel(NOW))).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void rechazarUnaAceptadaEsUnaTransicionInvalida() {
        Reservation accepted = pending(trip(driver(), 3));
        accepted.accept(NOW);

        assertThat(errorOf(() -> accepted.reject(NOW))).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void enUnViajeCanceladoNoSeRechazanSolicitudes() {
        Trip trip = trip(driver(), 3);
        Reservation reservation = pending(trip);
        close(trip, TripStatus.CANCELADO);

        assertThat(errorOf(() -> reservation.reject(NOW))).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void despuesDeLaSalidaNoCambianLasReservas() {
        Reservation reservation = pending(trip(driver(), 3));

        assertThat(errorOf(() -> reservation.accept(AFTER_DEPARTURE))).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDIENTE);
    }

    @Test
    void repetirUnaTransicionDespuesDeLaSalidaSigueSiendoIdempotente() {
        Reservation reservation = pending(trip(driver(), 3));
        reservation.accept(NOW);

        reservation.accept(AFTER_DEPARTURE);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACEPTADA);
    }

    @Test
    void cadaCambioDeUnaReservaActualizaElViaje() {
        Trip trip = trip(driver(), 3);
        Instant later = NOW.plusSeconds(60);
        Instant evenLater = NOW.plusSeconds(120);

        Reservation reservation = new Reservation(UUID.randomUUID(), trip, passenger(), later);
        assertThat(trip.getUpdatedAt()).isEqualTo(later);

        reservation.reject(evenLater);
        assertThat(trip.getUpdatedAt()).isEqualTo(evenLater);
    }

    @Test
    void reservaActivaEsPendienteOAceptada() {
        assertThat(ReservationStatus.PENDIENTE.isActive()).isTrue();
        assertThat(ReservationStatus.ACEPTADA.isActive()).isTrue();
        assertThat(ReservationStatus.RECHAZADA.isActive()).isFalse();
        assertThat(ReservationStatus.CANCELADA.isActive()).isFalse();
    }

    // --- Turno ---

    @Test
    void turnoSeCalculaConLasFranjasDeA0() {
        assertThat(Shift.of(LocalTime.of(6, 0))).isEqualTo(Shift.MANANA);
        assertThat(Shift.of(LocalTime.of(12, 59, 59))).isEqualTo(Shift.MANANA);
        assertThat(Shift.of(LocalTime.of(13, 0))).isEqualTo(Shift.TARDE);
        assertThat(Shift.of(LocalTime.of(17, 59))).isEqualTo(Shift.TARDE);
        assertThat(Shift.of(LocalTime.of(18, 0))).isEqualTo(Shift.NOCHE);
        assertThat(Shift.of(LocalTime.of(23, 59))).isEqualTo(Shift.NOCHE);
        assertThat(Shift.of(LocalTime.MIDNIGHT)).isEqualTo(Shift.NOCHE);
        assertThat(Shift.of(LocalTime.of(5, 59, 59))).isEqualTo(Shift.NOCHE);
    }
}

package com.example.uniride.backend.model;

import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.util.PublicName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static com.example.uniride.backend.TestData.LOCAL_NOW;
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
    void unViajeQueYaSalioNoAceptaSolicitudes() {
        assertThat(trip(driver(), 3).acceptsRequestsAt(LOCAL_NOW)).isTrue();
        assertThat(trip(driver(), 3, LocalDate.of(2026, 11, 19)).acceptsRequestsAt(LOCAL_NOW)).isFalse();
    }

    // --- Reserva ---

    @Test
    void aceptarTomaUnaVacanteYRepetirloNoTomaOtra() {
        Trip trip = trip(driver(), 3);
        Reservation reservation = pending(trip);

        reservation.accept(NOW);
        reservation.accept(NOW);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACEPTADA);
        assertThat(trip.getAvailableSeats()).isEqualTo(2);
    }

    @Test
    void sinVacantesLaReservaSigueSiendoPendiente() {
        Trip trip = trip(driver(), 1);
        pending(trip).accept(NOW);
        Reservation second = pending(trip);

        assertThat(errorOf(() -> second.accept(NOW))).isEqualTo(ErrorCode.NO_SEATS_AVAILABLE);
        assertThat(second.getStatus()).isEqualTo(ReservationStatus.PENDIENTE);
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
    void transicionesInvalidasDevuelvenInvalidStatusTransition() {
        Trip trip = trip(driver(), 3);
        Reservation rejected = pending(trip);
        rejected.reject(NOW);
        Reservation accepted = pending(trip);
        accepted.accept(NOW);

        assertThat(errorOf(() -> rejected.accept(NOW))).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(errorOf(() -> rejected.cancel(NOW))).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(errorOf(() -> accepted.reject(NOW))).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
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
        assertThat(Shift.of(LocalTime.of(0, 0))).isEqualTo(Shift.MANANA);
        assertThat(Shift.of(LocalTime.of(12, 59, 59))).isEqualTo(Shift.MANANA);
        assertThat(Shift.of(LocalTime.of(13, 0))).isEqualTo(Shift.TARDE);
        assertThat(Shift.of(LocalTime.of(17, 59))).isEqualTo(Shift.TARDE);
        assertThat(Shift.of(LocalTime.of(18, 0))).isEqualTo(Shift.NOCHE);
        assertThat(Shift.of(LocalTime.of(23, 59))).isEqualTo(Shift.NOCHE);
    }
}

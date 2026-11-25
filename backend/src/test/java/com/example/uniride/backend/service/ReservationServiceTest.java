package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.ReservationResponseDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.model.TripStatus;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.repository.TripRepository;
import com.example.uniride.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.uniride.backend.TestData.CLOCK;
import static com.example.uniride.backend.TestData.close;
import static com.example.uniride.backend.TestData.driver;
import static com.example.uniride.backend.TestData.fill;
import static com.example.uniride.backend.TestData.passenger;
import static com.example.uniride.backend.TestData.reservation;
import static com.example.uniride.backend.TestData.trip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orchestration of RF04: locks, who can do what and which error answers. The rules of each
 * transition are tested in ModelTest.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private UserRepository userRepository;

    private ReservationService service;
    private User driver;
    private User passenger;

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservationRepository, tripRepository, userRepository,
                new ViewMapper(new ContactVisibilityPolicy()), CLOCK);
        driver = driver();
        passenger = passenger();
        lenient().when(reservationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(userRepository.findById(passenger.getId())).thenReturn(Optional.of(passenger));
    }

    private void givenTrip(Trip trip) {
        lenient().when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        lenient().when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));
    }

    private void givenReservation(Reservation reservation) {
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
    }

    private void givenPassengerHasActiveReservation() {
        when(reservationRepository.existsByTripIdAndPassengerIdAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(true);
    }

    private static ErrorCode errorOf(Executable action) {
        return assertThrows(ApiException.class, action).getCode();
    }

    // --- Solicitar ---

    @Test
    void solicitarAsientoBloqueaElViajeYCreaLaReservaPendiente() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        UUID reservationId = UUID.randomUUID();

        var result = service.requestSeat(passenger.getId(), trip.getId(), reservationId);

        assertThat(result.created()).isTrue();
        assertThat(result.reservation().status()).isEqualTo(ReservationStatus.PENDIENTE);
        assertThat(result.reservation().passengerName()).isEqualTo("Martín G.");
        assertThat(trip.getAvailableSeats()).isEqualTo(3);
        verify(tripRepository).findByIdForUpdate(trip.getId());
    }

    @Test
    void reintentarLaMismaSolicitudDevuelveLaReservaSinDuplicarla() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation existing = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(existing);

        var result = service.requestSeat(passenger.getId(), trip.getId(), existing.getId());

        assertThat(result.created()).isFalse();
        assertThat(result.reservation().reservationId()).isEqualTo(existing.getId());
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void reusarElIdDeOtroUsuarioDevuelveIdConflict() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation someoneElses = reservation(trip, passenger(), ReservationStatus.PENDIENTE);
        givenReservation(someoneElses);

        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), trip.getId(), someoneElses.getId())))
                .isEqualTo(ErrorCode.ID_CONFLICT);
    }

    @Test
    void elConductorNoPuedeReservarEnSuPropioViaje() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);

        assertThat(errorOf(() -> service.requestSeat(driver.getId(), trip.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.CANNOT_RESERVE_OWN_TRIP);
    }

    @Test
    void solicitarEnUnViajeCompletoDevuelveNoSeatsAvailable() {
        Trip full = trip(driver, 1);
        fill(full);
        givenTrip(full);

        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), full.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.NO_SEATS_AVAILABLE);
    }

    @Test
    void solicitarEnUnViajeQueYaSalioDevuelveTripNotOpen() {
        Trip yesterday = trip(driver, 3, LocalDate.of(2026, 11, 19));
        givenTrip(yesterday);

        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), yesterday.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.TRIP_NOT_OPEN);
    }

    @Test
    void conUnaReservaActivaEnElViajeDevuelveAlreadyReserved() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        givenPassengerHasActiveReservation();

        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), trip.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.ALREADY_RESERVED);
    }

    @Test
    void quienLlenoElViajeConSuReservaRecibeAlreadyReservedYNoNoSeatsAvailable() {
        Trip trip = trip(driver, 1);
        fill(trip);
        givenTrip(trip);
        givenPassengerHasActiveReservation();

        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), trip.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.ALREADY_RESERVED);
    }

    @Test
    void siSaltaElIndiceDeReservaActivaDevuelveAlreadyReserved() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        when(reservationRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup",
                new SQLException("duplicate key violates \"" + Reservation.ONE_ACTIVE_PER_PASSENGER_INDEX + "\"", "23505")));

        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), trip.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.ALREADY_RESERVED);
    }

    // --- Aceptar y rechazar ---

    @Test
    void aceptarBloqueaElViajeYDescuentaUnaVacante() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(pending);

        ReservationResponseDTO response = service.accept(driver.getId(), trip.getId(), pending.getId());

        assertThat(response.status()).isEqualTo(ReservationStatus.ACEPTADA);
        assertThat(response.passengerPhone()).isEqualTo("+5491155550001");
        assertThat(trip.getAvailableSeats()).isEqualTo(2);
        verify(tripRepository).findByIdForUpdate(trip.getId());
    }

    @Test
    void aceptarReservaSinVacantesDevuelveConflicto() {
        Trip trip = trip(driver, 1);
        fill(trip);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(pending);

        assertThat(errorOf(() -> service.accept(driver.getId(), trip.getId(), pending.getId())))
                .isEqualTo(ErrorCode.NO_SEATS_AVAILABLE);
    }

    @Test
    void soloElConductorPuedeAceptar() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);

        assertThat(errorOf(() -> service.accept(passenger.getId(), trip.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void soloElConductorPuedeRechazar() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);

        assertThat(errorOf(() -> service.reject(passenger.getId(), trip.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void unaReservaDeOtroViajeNoSeEncuentra() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation fromAnotherTrip = reservation(trip(driver, 3), passenger, ReservationStatus.PENDIENTE);
        givenReservation(fromAnotherTrip);

        assertThat(errorOf(() -> service.accept(driver.getId(), trip.getId(), fromAnotherTrip.getId())))
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void despuesDeLaSalidaNoSeAcepta() {
        Trip departed = trip(driver, 3, LocalDate.of(2026, 11, 19));
        givenTrip(departed);
        Reservation pending = new Reservation(UUID.randomUUID(), departed, passenger,
                CLOCK.instant().minusSeconds(86_400));
        givenReservation(pending);

        assertThat(errorOf(() -> service.accept(driver.getId(), departed.getId(), pending.getId())))
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void enUnViajeCanceladoNoSeRechaza() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        close(trip, TripStatus.CANCELADO);
        givenReservation(pending);

        assertThat(errorOf(() -> service.reject(driver.getId(), trip.getId(), pending.getId())))
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    // --- Cancelar ---

    @Test
    void cancelarUnaAceptadaDevuelveLaVacante() {
        Trip trip = trip(driver, 1);
        givenTrip(trip);
        Reservation accepted = reservation(trip, passenger, ReservationStatus.ACEPTADA);
        givenReservation(accepted);

        ReservationResponseDTO response = service.cancel(passenger.getId(), trip.getId(), accepted.getId());

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELADA);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.ABIERTO);
        assertThat(trip.getAvailableSeats()).isEqualTo(1);
    }

    @Test
    void soloElPasajeroPuedeCancelarSuReserva() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(pending);

        assertThat(errorOf(() -> service.cancel(driver.getId(), trip.getId(), pending.getId())))
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    // --- Listar ---

    @Test
    void elConductorVeElTelefonoSoloDeLasReservasAceptadas() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        when(reservationRepository.findByTripIdOrderByReservedAtAsc(trip.getId())).thenReturn(List.of(
                reservation(trip, passenger, ReservationStatus.ACEPTADA),
                reservation(trip, passenger(), ReservationStatus.PENDIENTE)));

        List<ReservationResponseDTO> list = service.listForTrip(driver.getId(), trip.getId());

        assertThat(list).extracting(ReservationResponseDTO::passengerPhone).containsExactly("+5491155550001", null);
    }

    @Test
    void elPasajeroNoPuedeVerLasSolicitudesDelViaje() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);

        assertThat(errorOf(() -> service.listForTrip(passenger.getId(), trip.getId())))
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}

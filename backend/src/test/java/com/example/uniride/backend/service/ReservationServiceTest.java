package com.example.uniride.backend.service;

import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.dto.ReservationDTO;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.repository.TripRepository;
import com.example.uniride.backend.model.TripStatus;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.uniride.backend.TestData.CLOCK;
import static com.example.uniride.backend.TestData.driver;
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

/** Orchestration of RF04: locks, permissions and errors. The transitions are in ModelTest. */
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
                new ContactVisibilityPolicy(), CLOCK);
        driver = driver();
        passenger = passenger();
        lenient().when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenTrip(Trip trip) {
        lenient().when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        lenient().when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));
    }

    private void givenReservation(Reservation reservation) {
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
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
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());
        when(userRepository.findById(passenger.getId())).thenReturn(Optional.of(passenger));

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
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reusarElIdDeOtroUsuarioDevuelveIdConflict() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation existing = reservation(trip, passenger(), ReservationStatus.PENDIENTE);
        givenReservation(existing);

        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), trip.getId(), existing.getId())))
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
    void solicitarEnUnViajeCompletoOQueYaSalioDevuelveTripNotOpen() {
        Trip full = trip(driver, 1);
        reservation(full, passenger(), ReservationStatus.ACEPTADA);
        givenTrip(full);
        Trip yesterday = trip(driver, 3, LocalDate.of(2026, 11, 19));
        givenTrip(yesterday);

        assertThat(full.getStatus()).isEqualTo(TripStatus.COMPLETO);
        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), full.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.TRIP_NOT_OPEN);
        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), yesterday.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.TRIP_NOT_OPEN);
    }

    @Test
    void conUnaReservaActivaEnElViajeDevuelveAlreadyReserved() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        when(reservationRepository.existsByTripIdAndPassengerIdAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(true);

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

        ReservationDTO response = service.accept(driver.getId(), trip.getId(), pending.getId());

        assertThat(response.status()).isEqualTo(ReservationStatus.ACEPTADA);
        assertThat(response.passengerPhone()).isEqualTo("+5491155550001");
        assertThat(trip.getAvailableSeats()).isEqualTo(2);
        verify(tripRepository).findByIdForUpdate(trip.getId());
    }

    @Test
    void aceptarReservaSinVacantesDevuelveConflicto() {
        Trip trip = trip(driver, 1);
        reservation(trip, passenger(), ReservationStatus.ACEPTADA);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(pending);

        assertThat(errorOf(() -> service.accept(driver.getId(), trip.getId(), pending.getId())))
                .isEqualTo(ErrorCode.NO_SEATS_AVAILABLE);
    }

    @Test
    void soloElConductorPuedeAceptarYRechazar() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);

        assertThat(errorOf(() -> service.accept(passenger.getId(), trip.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(errorOf(() -> service.reject(passenger.getId(), trip.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void unaReservaDeOtroViajeNoSeEncuentra() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation other = reservation(trip(driver, 3), passenger, ReservationStatus.PENDIENTE);
        givenReservation(other);

        assertThat(errorOf(() -> service.accept(driver.getId(), trip.getId(), other.getId())))
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void despuesDeLaSalidaNoSeAceptaPeroElReintentoSiResponde() {
        Trip departed = trip(driver, 3, LocalDate.of(2026, 11, 19));
        givenTrip(departed);
        Reservation pending = reservation(departed, passenger, ReservationStatus.PENDIENTE);
        Reservation accepted = reservation(departed, passenger(), ReservationStatus.ACEPTADA);

        givenReservation(pending);
        assertThat(errorOf(() -> service.accept(driver.getId(), departed.getId(), pending.getId())))
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);

        givenReservation(accepted);
        assertThat(service.accept(driver.getId(), departed.getId(), accepted.getId()).status())
                .isEqualTo(ReservationStatus.ACEPTADA);
    }

    // --- Cancelar ---

    @Test
    void cancelarUnaAceptadaDevuelveLaVacante() {
        Trip trip = trip(driver, 1);
        givenTrip(trip);
        Reservation accepted = reservation(trip, passenger, ReservationStatus.ACEPTADA);
        givenReservation(accepted);

        ReservationDTO response = service.cancel(passenger.getId(), trip.getId(), accepted.getId());

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

    @Test
    void despuesDeLaSalidaNoSePuedeCancelar() {
        Trip departed = trip(driver, 3, LocalDate.of(2026, 11, 19));
        givenTrip(departed);
        Reservation accepted = reservation(departed, passenger, ReservationStatus.ACEPTADA);
        givenReservation(accepted);

        assertThat(errorOf(() -> service.cancel(passenger.getId(), departed.getId(), accepted.getId())))
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(departed.getAvailableSeats()).isEqualTo(2);
    }

    // --- Listar ---

    @Test
    void elConductorVeElTelefonoSoloDeLasReservasAceptadas() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        when(reservationRepository.findByTripIdOrderByReservedAtAsc(trip.getId())).thenReturn(List.of(
                reservation(trip, passenger, ReservationStatus.ACEPTADA),
                reservation(trip, passenger(), ReservationStatus.PENDIENTE)));

        List<ReservationDTO> list = service.listForTrip(driver.getId(), trip.getId());

        assertThat(list).extracting(ReservationDTO::passengerPhone).containsExactly("+5491155550001", null);
    }

    @Test
    void elPasajeroNoPuedeVerLasSolicitudesDelViaje() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);

        assertThat(errorOf(() -> service.listForTrip(passenger.getId(), trip.getId())))
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}

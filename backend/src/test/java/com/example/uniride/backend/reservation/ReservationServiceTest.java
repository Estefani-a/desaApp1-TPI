package com.example.uniride.backend.reservation;

import com.example.uniride.backend.common.error.ApiException;
import com.example.uniride.backend.common.error.ErrorCode;
import com.example.uniride.backend.reservation.dto.ReservationResponse;
import com.example.uniride.backend.trip.Trip;
import com.example.uniride.backend.trip.TripRepository;
import com.example.uniride.backend.trip.TripStatus;
import com.example.uniride.backend.user.User;
import com.example.uniride.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        service = new ReservationService(reservationRepository, tripRepository, userRepository, CLOCK);
        driver = driver();
        passenger = passenger();
        lenient().when(reservationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenTrip(Trip trip) {
        lenient().when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        lenient().when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));
    }

    private void givenReservation(Reservation reservation) {
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
    }

    private ErrorCode errorOf(Runnable action) {
        try {
            action.run();
        } catch (ApiException ex) {
            return ex.getCode();
        }
        throw new AssertionError("Se esperaba una ApiException");
    }

    // --- Solicitar ---

    @Test
    void solicitarAsientoCreaLaReservaPendienteSinDescontarVacante() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        UUID reservationId = UUID.randomUUID();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());
        when(userRepository.findById(passenger.getId())).thenReturn(Optional.of(passenger));

        var result = service.requestSeat(passenger.getId(), trip.getId(), reservationId);

        assertThat(result.created()).isTrue();
        assertThat(result.reservation().reservationId()).isEqualTo(reservationId);
        assertThat(result.reservation().status()).isEqualTo(ReservationStatus.PENDIENTE);
        assertThat(result.reservation().passengerName()).isEqualTo("Martín G.");
        assertThat(trip.getAvailableSeats()).isEqualTo(3);
    }

    @Test
    void reintentarLaMismaSolicitudDevuelveLaReservaSinDuplicarla() {
        Trip trip = trip(driver, 3);
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
    void solicitarEnUnViajeCompletoOCanceladoDevuelveTripNotOpen() {
        Trip full = trip(driver, 1);
        full.setAvailableSeats(0);
        full.setStatus(TripStatus.COMPLETO);
        givenTrip(full);
        Trip cancelled = trip(driver, 3);
        cancelled.setStatus(TripStatus.CANCELADO);
        givenTrip(cancelled);

        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), full.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.TRIP_NOT_OPEN);
        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), cancelled.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.TRIP_NOT_OPEN);
    }

    @Test
    void solicitarEnUnViajeQueYaSalioDevuelveTripNotOpen() {
        Trip yesterday = new Trip(UUID.randomUUID(), driver, trip(driver, 3).getDirection(),
                trip(driver, 3).getCampus(), "Salida Lima 717", "Ramos Mejía", null,
                LocalDate.of(2026, 11, 19), trip(driver, 3).getDepartureTime(), null, 3, 3500,
                "Gol Trend", "Gris", "AB123CD");
        givenTrip(yesterday);

        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), yesterday.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.TRIP_NOT_OPEN);
    }

    @Test
    void conUnaReservaActivaEnElViajeDevuelveAlreadyReserved() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        when(reservationRepository.existsByTripIdAndPassengerIdAndStatusIn(
                any(), any(), anyCollection())).thenReturn(true);

        assertThat(errorOf(() -> service.requestSeat(passenger.getId(), trip.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.ALREADY_RESERVED);
    }

    // --- Aceptar ---

    @Test
    void aceptarDescuentaUnaVacanteUsandoElBloqueoDeLaFila() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(pending);

        ReservationResponse response = service.accept(driver.getId(), trip.getId(), pending.getId());

        assertThat(response.status()).isEqualTo(ReservationStatus.ACEPTADA);
        assertThat(trip.getAvailableSeats()).isEqualTo(2);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.ABIERTO);
        verify(tripRepository).findByIdForUpdate(trip.getId());
    }

    @Test
    void aceptarLaUltimaVacanteDejaElViajeCompleto() {
        Trip trip = trip(driver, 1);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(pending);

        service.accept(driver.getId(), trip.getId(), pending.getId());

        assertThat(trip.getAvailableSeats()).isZero();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETO);
    }

    @Test
    void aceptarReservaSinVacantesDevuelveConflicto() {
        Trip trip = trip(driver, 1);
        trip.setAvailableSeats(0);
        trip.setStatus(TripStatus.COMPLETO);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(pending);

        assertThat(errorOf(() -> service.accept(driver.getId(), trip.getId(), pending.getId())))
                .isEqualTo(ErrorCode.NO_SEATS_AVAILABLE);
        assertThat(pending.getStatus()).isEqualTo(ReservationStatus.PENDIENTE);
    }

    @Test
    void aceptarDosVecesNoDescuentaDosVacantes() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(pending);

        service.accept(driver.getId(), trip.getId(), pending.getId());
        service.accept(driver.getId(), trip.getId(), pending.getId());

        assertThat(trip.getAvailableSeats()).isEqualTo(2);
    }

    @Test
    void soloElConductorPuedeAceptar() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);

        assertThat(errorOf(() -> service.accept(passenger.getId(), trip.getId(), UUID.randomUUID())))
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void aceptarUnaReservaCanceladaDevuelveInvalidStatusTransition() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation cancelled = reservation(trip, passenger, ReservationStatus.CANCELADA);
        givenReservation(cancelled);

        assertThat(errorOf(() -> service.accept(driver.getId(), trip.getId(), cancelled.getId())))
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
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

    // --- Rechazar ---

    @Test
    void rechazarNoTocaLasVacantes() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(pending);

        ReservationResponse response = service.reject(driver.getId(), trip.getId(), pending.getId());

        assertThat(response.status()).isEqualTo(ReservationStatus.RECHAZADA);
        assertThat(trip.getAvailableSeats()).isEqualTo(3);
    }

    @Test
    void rechazarUnaAceptadaDevuelveInvalidStatusTransition() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation accepted = reservation(trip, passenger, ReservationStatus.ACEPTADA);
        givenReservation(accepted);

        assertThat(errorOf(() -> service.reject(driver.getId(), trip.getId(), accepted.getId())))
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    // --- Cancelar ---

    @Test
    void cancelarUnaAceptadaDevuelveLaVacanteYReabreElViaje() {
        Trip trip = trip(driver, 1);
        trip.setAvailableSeats(0);
        trip.setStatus(TripStatus.COMPLETO);
        givenTrip(trip);
        Reservation accepted = reservation(trip, passenger, ReservationStatus.ACEPTADA);
        givenReservation(accepted);

        ReservationResponse response = service.cancel(passenger.getId(), trip.getId(), accepted.getId());

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELADA);
        assertThat(trip.getAvailableSeats()).isEqualTo(1);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.ABIERTO);
    }

    @Test
    void cancelarUnaPendienteNoTocaLasVacantesYEsIdempotente() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        givenReservation(pending);

        service.cancel(passenger.getId(), trip.getId(), pending.getId());
        ReservationResponse again = service.cancel(passenger.getId(), trip.getId(), pending.getId());

        assertThat(again.status()).isEqualTo(ReservationStatus.CANCELADA);
        assertThat(trip.getAvailableSeats()).isEqualTo(3);
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

    // --- Listar y privacidad (RNF07) ---

    @Test
    void elConductorVeElTelefonoSoloDeLasReservasAceptadas() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        when(reservationRepository.findByTripIdOrderByReservedAtAsc(trip.getId())).thenReturn(List.of(
                reservation(trip, passenger, ReservationStatus.ACEPTADA),
                reservation(trip, passenger(), ReservationStatus.PENDIENTE)));

        List<ReservationResponse> list = service.listForTrip(driver.getId(), trip.getId());

        assertThat(list).extracting(ReservationResponse::passengerPhone)
                .containsExactly("+5491155550001", null);
    }

    @Test
    void elPasajeroNoPuedeVerLasSolicitudesDelViaje() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);

        assertThatThrownBy(() -> service.listForTrip(passenger.getId(), trip.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}

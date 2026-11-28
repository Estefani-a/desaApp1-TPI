package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.PublishTripRequestDTO;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.Direction;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.model.TripStatus;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.CampusRepository;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.repository.TripRepository;
import com.example.uniride.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.uniride.backend.TestData.CLOCK;
import static com.example.uniride.backend.TestData.MONSERRAT;
import static com.example.uniride.backend.TestData.NOW;
import static com.example.uniride.backend.TestData.close;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Publishing, finishing, cancelling and "Mis viajes" (RF02): validations, who can do what and
 * which error answers. "Now" is 2026-11-20 18:00 in Buenos Aires.
 */
@ExtendWith(MockitoExtension.class)
class TripPublicationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    @Mock
    private TripRepository tripRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CampusRepository campusRepository;

    private TripPublicationService service;
    private User driver;
    private User passenger;

    @BeforeEach
    void setUp() {
        service = new TripPublicationService(tripRepository, reservationRepository, userRepository,
                campusRepository, new ViewMapper(new ContactVisibilityPolicy()), CLOCK);
        driver = driver();
        passenger = passenger();
        lenient().when(userRepository.findByIdForUpdate(driver.getId())).thenReturn(Optional.of(driver));
        lenient().when(userRepository.findByIdForUpdate(passenger.getId())).thenReturn(Optional.of(passenger));
        lenient().when(campusRepository.findById("MONSERRAT")).thenReturn(Optional.of(MONSERRAT));
        lenient().when(tripRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static PublishTripRequestDTO request(String campus, String meetingPoint, LocalDate date, LocalTime time) {
        return new PublishTripRequestDTO(Direction.VUELTA, campus, meetingPoint, "  Ramos Mejía ", "  ",
                date, time, LocalTime.of(23, 0), 3, 3500);
    }

    private static PublishTripRequestDTO validRequest() {
        return request("MONSERRAT", "Salida Lima 717", TODAY, LocalTime.of(22, 15));
    }

    private void givenTrip(Trip trip) {
        lenient().when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        lenient().when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));
    }

    private static ApiException errorFrom(Executable action) {
        return assertThrows(ApiException.class, action);
    }

    private static ErrorCode errorOf(Executable action) {
        return errorFrom(action).getCode();
    }

    // --- Publicar ---

    @Test
    void publicarCreaElViajeAbiertoConLosDatosDelPerfil() {
        UUID tripId = UUID.randomUUID();

        var result = service.publish(driver.getId(), tripId, validRequest());

        assertThat(result.created()).isTrue();
        TripResponseDTO trip = result.trip();
        assertThat(trip.tripId()).isEqualTo(tripId);
        assertThat(trip.driverId()).isEqualTo(driver.getId());
        assertThat(trip.driverName()).isEqualTo("Lucía F.");
        assertThat(trip.status()).isEqualTo(TripStatus.ABIERTO);
        assertThat(trip.availableSeats()).isEqualTo(3);
        assertThat(trip.vehicleModel()).isEqualTo("Gol Trend");
        assertThat(trip.vehiclePlate()).isEqualTo("AB123CD");
        assertThat(trip.driverPhone()).isEqualTo("+5491155550000");
        assertThat(trip.destinationNeighborhood()).isEqualTo("Ramos Mejía");
        assertThat(trip.destinationReference()).isNull();
        assertThat(trip.myReservation()).isNull();
        assertThat(trip.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void dosPutIgualesCreanUnSoloViaje() {
        UUID tripId = UUID.randomUUID();
        var first = service.publish(driver.getId(), tripId, validRequest());
        var saved = org.mockito.ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(saved.capture());
        givenTrip(saved.getValue());

        var retry = service.publish(driver.getId(), tripId, validRequest());

        assertThat(retry.created()).isFalse();
        assertThat(retry.trip()).isEqualTo(first.trip());
        verify(tripRepository, times(1)).save(any());
    }

    @Test
    void elReintentoDevuelveElViajeAunqueYaHayaSalido() {
        Trip existing = trip(driver, 3, TODAY.minusDays(1));
        givenTrip(existing);

        var result = service.publish(driver.getId(), existing.getId(),
                request("MONSERRAT", "Salida Lima 717", TODAY.minusDays(1), LocalTime.of(22, 15)));

        assertThat(result.created()).isFalse();
        verify(tripRepository, never()).save(any());
    }

    @Test
    void publicarBloqueaAlConductorAntesDeBuscarElViaje() {
        UUID tripId = UUID.randomUUID();

        service.publish(driver.getId(), tripId, validRequest());

        var order = org.mockito.Mockito.inOrder(userRepository, tripRepository);
        order.verify(userRepository).findByIdForUpdate(driver.getId());
        order.verify(tripRepository).findById(tripId);
    }

    @Test
    void elIdDeUnViajeDeOtroUsuarioDevuelveIdConflict() {
        Trip someoneElses = trip(driver(), 3);
        givenTrip(someoneElses);

        assertThat(errorOf(() -> service.publish(driver.getId(), someoneElses.getId(), validRequest())))
                .isEqualTo(ErrorCode.ID_CONFLICT);
    }

    @Test
    void sinVehiculoNoSePuedePublicar() {
        ApiException error = errorFrom(() -> service.publish(passenger.getId(), UUID.randomUUID(), validRequest()));

        assertThat(error.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(error.getFields()).containsOnlyKeys("vehicle");
        verify(tripRepository, never()).save(any());
    }

    @Test
    void laSalidaTieneQueSerPosteriorAAhoraEnBuenosAires() {
        for (LocalTime time : List.of(LocalTime.of(17, 59), LocalTime.of(18, 0))) {
            ApiException error = errorFrom(() -> service.publish(driver.getId(), UUID.randomUUID(),
                    request("MONSERRAT", "Salida Lima 717", TODAY, time)));

            assertThat(error.getFields()).containsOnlyKeys("departureDate");
        }
        assertThat(service.publish(driver.getId(), UUID.randomUUID(),
                request("MONSERRAT", "Salida Lima 717", TODAY, LocalTime.of(18, 1))).created()).isTrue();
    }

    @Test
    void laSedeTieneQueExistir() {
        ApiException error = errorFrom(() -> service.publish(driver.getId(), UUID.randomUUID(),
                request("PALERMO", "Salida Lima 717", TODAY, LocalTime.of(22, 15))));

        assertThat(error.getFields()).containsOnlyKeys("campus");
    }

    @Test
    void elPuntoDeEncuentroTieneQueSerDeLaSede() {
        ApiException error = errorFrom(() -> service.publish(driver.getId(), UUID.randomUUID(),
                request("MONSERRAT", "Salida Libertad 1340", TODAY, LocalTime.of(22, 15))));

        assertThat(error.getFields()).containsOnlyKeys("meetingPoint");
    }

    @Test
    void variosErroresSeInformanJuntos() {
        ApiException error = errorFrom(() -> service.publish(passenger.getId(), UUID.randomUUID(),
                request("PALERMO", "Otra", TODAY.minusDays(1), LocalTime.of(22, 15))));

        assertThat(error.getFields()).containsOnlyKeys("vehicle", "departureDate", "campus");
    }

    @Test
    void unUsuarioQueYaNoExisteRecibeUnauthenticated() {
        assertThat(errorOf(() -> service.publish(UUID.randomUUID(), UUID.randomUUID(), validRequest())))
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    // --- Finalizar y cancelar ---

    @Test
    void finalizarDejaElViajeFinalizadoYNoTocaLasReservas() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);

        TripResponseDTO response = service.finish(driver.getId(), trip.getId());

        assertThat(response.status()).isEqualTo(TripStatus.FINALIZADO);
        assertThat(pending.getStatus()).isEqualTo(ReservationStatus.PENDIENTE);
        verify(tripRepository).findByIdForUpdate(trip.getId());
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void cancelarCancelaLasReservasPendientesYAceptadasSinDevolverVacantes() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        Reservation accepted = reservation(trip, passenger(), ReservationStatus.ACEPTADA);
        when(reservationRepository.findByTripIdAndStatusIn(trip.getId(), ReservationStatus.ACTIVE))
                .thenReturn(List.of(pending, accepted));

        TripResponseDTO response = service.cancel(driver.getId(), trip.getId());

        assertThat(response.status()).isEqualTo(TripStatus.CANCELADO);
        assertThat(response.availableSeats()).isEqualTo(2);
        assertThat(pending.getStatus()).isEqualTo(ReservationStatus.CANCELADA);
        assertThat(accepted.getStatus()).isEqualTo(ReservationStatus.CANCELADA);
    }

    @Test
    void unViajeQueYaSalioSePuedeCancelarYFinalizar() {
        Trip departed = trip(driver, 3, TODAY.minusDays(1));
        Trip other = trip(driver, 3, TODAY.minusDays(1));
        givenTrip(departed);
        givenTrip(other);
        Reservation pending = reservation(departed, passenger, ReservationStatus.PENDIENTE);
        when(reservationRepository.findByTripIdAndStatusIn(departed.getId(), ReservationStatus.ACTIVE))
                .thenReturn(List.of(pending));

        assertThat(service.cancel(driver.getId(), departed.getId()).status()).isEqualTo(TripStatus.CANCELADO);
        assertThat(pending.getStatus()).isEqualTo(ReservationStatus.CANCELADA);
        assertThat(service.finish(driver.getId(), other.getId()).status()).isEqualTo(TripStatus.FINALIZADO);
    }

    @Test
    void soloElConductorPuedeFinalizarOCancelar() {
        Trip trip = trip(driver, 3);
        givenTrip(trip);

        assertThat(errorOf(() -> service.finish(passenger.getId(), trip.getId()))).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(errorOf(() -> service.cancel(passenger.getId(), trip.getId()))).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.ABIERTO);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void finalizarOCancelarUnViajeInexistenteDevuelveNotFound() {
        assertThat(errorOf(() -> service.finish(driver.getId(), UUID.randomUUID()))).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(errorOf(() -> service.cancel(driver.getId(), UUID.randomUUID()))).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void noSePuedeFinalizarUnCanceladoNiCancelarUnFinalizado() {
        Trip cancelled = trip(driver, 3);
        close(cancelled, TripStatus.CANCELADO);
        Trip finished = trip(driver, 3);
        close(finished, TripStatus.FINALIZADO);
        givenTrip(cancelled);
        givenTrip(finished);

        assertThat(errorOf(() -> service.finish(driver.getId(), cancelled.getId())))
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(errorOf(() -> service.cancel(driver.getId(), finished.getId())))
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void repetirLaMismaTransicionDevuelveElViajeSinCambios() {
        Trip cancelled = trip(driver, 3);
        close(cancelled, TripStatus.CANCELADO);
        Trip finished = trip(driver, 3);
        close(finished, TripStatus.FINALIZADO);
        givenTrip(cancelled);
        givenTrip(finished);

        assertThat(service.cancel(driver.getId(), cancelled.getId()).status()).isEqualTo(TripStatus.CANCELADO);
        assertThat(service.finish(driver.getId(), finished.getId()).status()).isEqualTo(TripStatus.FINALIZADO);
        verifyNoInteractions(reservationRepository);
    }

    // --- Mis viajes ---

    @Test
    void misViajesPideLosCerradosDesdeHaceSieteDias() {
        service.listMine(driver.getId());

        verify(tripRepository).findMine(driver.getId(), TripStatus.CLOSED, LocalDate.of(2026, 11, 13));
        verify(reservationRepository, never()).findByPassengerIdAndTripIdIn(any(), anyCollection());
    }

    @Test
    void misViajesTraeMiReservaConUnaSolaConsultaYAplicaLaPrivacidad() {
        Trip accepted = trip(driver, 3);
        Trip rejected = trip(driver, 3);
        Reservation old = reservation(accepted, passenger, ReservationStatus.CANCELADA);
        Reservation current = reservation(accepted, passenger, ReservationStatus.ACEPTADA);
        Reservation no = reservation(rejected, passenger, ReservationStatus.RECHAZADA);
        when(tripRepository.findMine(any(), anyCollection(), any())).thenReturn(List.of(accepted, rejected));
        when(reservationRepository.findByPassengerIdAndTripIdIn(passenger.getId(),
                List.of(accepted.getId(), rejected.getId()))).thenReturn(List.of(current, old, no));

        List<TripResponseDTO> mine = service.listMine(passenger.getId());

        assertThat(mine).extracting(TripResponseDTO::tripId).containsExactly(accepted.getId(), rejected.getId());
        assertThat(mine.get(0).myReservation().reservationId()).isEqualTo(current.getId());
        assertThat(mine.get(0).vehiclePlate()).isEqualTo("AB123CD");
        assertThat(mine.get(1).myReservation().status()).isEqualTo(ReservationStatus.RECHAZADA);
        assertThat(mine.get(1).vehiclePlate()).isNull();
        assertThat(mine.get(1).driverPhone()).isNull();
        verify(reservationRepository, times(1)).findByPassengerIdAndTripIdIn(any(), anyCollection());
    }
}

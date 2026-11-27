package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.TripFiltersRequestDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.repository.TripRepository;
import com.example.uniride.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.uniride.backend.TestData.CLOCK;
import static com.example.uniride.backend.TestData.driver;
import static com.example.uniride.backend.TestData.noFilters;
import static com.example.uniride.backend.TestData.passenger;
import static com.example.uniride.backend.TestData.reservation;
import static com.example.uniride.backend.TestData.trip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Trip list and detail: the RNF07 policy is applied and "my reservation" is the right one. */
@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private ReservationRepository reservationRepository;

    private TripService service;
    private User driver;
    private User passenger;
    private Trip trip;

    @BeforeEach
    void setUp() {
        service = new TripService(tripRepository, reservationRepository,
                new ViewMapper(new ContactVisibilityPolicy()), CLOCK);
        driver = driver();
        passenger = passenger();
        trip = trip(driver, 3);
        lenient().when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
    }

    private void givenMyReservations(ReservationStatus... statuses) {
        var list = Arrays.stream(statuses).map(status -> reservation(trip, passenger, status)).toList();
        when(reservationRepository.findByTripIdAndPassengerIdOrderByReservedAtDesc(trip.getId(), passenger.getId()))
                .thenReturn(list);
    }

    @Test
    void conReservaPendienteNoSeVenPatenteNiTelefono() {
        givenMyReservations(ReservationStatus.PENDIENTE);

        TripResponseDTO response = service.getDetail(passenger.getId(), trip.getId());

        assertThat(response.vehiclePlate()).isNull();
        assertThat(response.driverPhone()).isNull();
        assertThat(response.driverName()).isEqualTo("Lucía F.");
        assertThat(response.myReservation().status()).isEqualTo(ReservationStatus.PENDIENTE);
    }

    @Test
    void conReservaAceptadaSeVenPatenteYTelefono() {
        givenMyReservations(ReservationStatus.ACEPTADA);

        TripResponseDTO response = service.getDetail(passenger.getId(), trip.getId());

        assertThat(response.vehiclePlate()).isEqualTo("AB123CD");
        assertThat(response.driverPhone()).isEqualTo("+5491155550000");
    }

    @Test
    void miReservaEsLaActivaAunqueHayaUnaRechazadaMasNueva() {
        givenMyReservations(ReservationStatus.RECHAZADA, ReservationStatus.PENDIENTE);

        TripResponseDTO response = service.getDetail(passenger.getId(), trip.getId());

        assertThat(response.myReservation().status()).isEqualTo(ReservationStatus.PENDIENTE);
    }

    @Test
    void sinReservaMiReservaEsNull() {
        when(reservationRepository.findByTripIdAndPassengerIdOrderByReservedAtDesc(trip.getId(), passenger.getId()))
                .thenReturn(List.of());

        assertThat(service.getDetail(passenger.getId(), trip.getId()).myReservation()).isNull();
    }

    @Test
    void viajeInexistenteDevuelveNotFound() {
        UUID unknown = UUID.randomUUID();
        when(tripRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(passenger.getId(), unknown))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    // --- Listado (RF03) ---

    @SuppressWarnings("unchecked")
    private void givenListedTrips(Trip... trips) {
        when(tripRepository.findTrips(any(Specification.class), any(Sort.class), any(Limit.class)))
                .thenReturn(List.of(trips));
    }

    @SuppressWarnings("unchecked")
    private void verifyQuery(Sort sort, Limit limit) {
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        ArgumentCaptor<Limit> limitCaptor = ArgumentCaptor.forClass(Limit.class);
        verify(tripRepository).findTrips(any(Specification.class), sortCaptor.capture(), limitCaptor.capture());
        assertThat(sortCaptor.getValue()).isEqualTo(sort);
        assertThat(limitCaptor.getValue()).isEqualTo(limit);
    }

    @Test
    void sinUpdatedSinceTraeComoMaximoCienViajesOrdenadosPorSalidaYId() {
        givenListedTrips();

        service.list(passenger.getId(), noFilters());

        verifyQuery(Sort.by("schedule.departureDate", "schedule.departureTime", "id"), Limit.of(100));
    }

    @Test
    void conUpdatedSinceOrdenaPorCambioYNoCorta() {
        givenListedTrips();

        service.list(passenger.getId(), new TripFiltersRequestDTO(null, null, null, null,
                OffsetDateTime.parse("2026-11-20T18:00:00-03:00")));

        verifyQuery(Sort.by("updatedAt", "id"), Limit.unlimited());
    }

    @Test
    void elListadoVacioNoConsultaReservas() {
        givenListedTrips();

        assertThat(service.list(passenger.getId(), noFilters())).isEmpty();
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void elListadoNoMuestraPatenteNiTelefonoSinReservaAceptada() {
        givenListedTrips(trip);
        when(reservationRepository.findByPassengerIdAndTripIdIn(eq(passenger.getId()), anyCollection()))
                .thenReturn(List.of());

        TripResponseDTO listed = service.list(passenger.getId(), noFilters()).getFirst();

        assertThat(listed.vehiclePlate()).isNull();
        assertThat(listed.driverPhone()).isNull();
        assertThat(listed.myReservation()).isNull();
        assertThat(listed.driverName()).isEqualTo("Lucía F.");
    }

    @Test
    void cadaViajeDelListadoTraeMiReserva() {
        Trip other = trip(driver, 2);
        givenListedTrips(trip, other);
        when(reservationRepository.findByPassengerIdAndTripIdIn(eq(passenger.getId()), anyCollection()))
                .thenReturn(List.of(
                        reservation(trip, passenger, ReservationStatus.RECHAZADA),
                        reservation(trip, passenger, ReservationStatus.ACEPTADA)));

        List<TripResponseDTO> listed = service.list(passenger.getId(), noFilters());

        assertThat(listed).extracting(TripResponseDTO::tripId).containsExactly(trip.getId(), other.getId());
        assertThat(listed.get(0).myReservation().status()).isEqualTo(ReservationStatus.ACEPTADA);
        assertThat(listed.get(0).vehiclePlate()).isEqualTo("AB123CD");
        assertThat(listed.get(1).myReservation()).isNull();
        assertThat(listed.get(1).vehiclePlate()).isNull();
    }

    @Test
    void lasReservasDeTodoElListadoSePidenEnUnaSolaConsulta() {
        Trip other = trip(driver, 2);
        givenListedTrips(trip, other);

        service.list(passenger.getId(), noFilters());

        verify(reservationRepository).findByPassengerIdAndTripIdIn(passenger.getId(), List.of(trip.getId(), other.getId()));
        verifyNoMoreInteractions(reservationRepository);
    }

    @Test
    void elConductorVeSusDatosEnElListado() {
        givenListedTrips(trip);
        when(reservationRepository.findByPassengerIdAndTripIdIn(eq(driver.getId()), anyCollection()))
                .thenReturn(List.of());

        TripResponseDTO listed = service.list(driver.getId(), noFilters()).getFirst();

        assertThat(listed.vehiclePlate()).isEqualTo("AB123CD");
        assertThat(listed.driverPhone()).isEqualTo("+5491155550000");
    }
}

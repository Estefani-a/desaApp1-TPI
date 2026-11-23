package com.example.uniride.backend.service;

import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.dto.TripDTO;
import com.example.uniride.backend.repository.TripRepository;
import com.example.uniride.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.uniride.backend.TestData.driver;
import static com.example.uniride.backend.TestData.passenger;
import static com.example.uniride.backend.TestData.reservation;
import static com.example.uniride.backend.TestData.trip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Trip detail: the policy is applied and "my reservation" is the right one. */
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
        service = new TripService(tripRepository, reservationRepository, new ContactVisibilityPolicy());
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

        TripDTO response = service.getDetail(passenger.getId(), trip.getId());

        assertThat(response.vehiclePlate()).isNull();
        assertThat(response.driverPhone()).isNull();
        assertThat(response.driverName()).isEqualTo("Lucía F.");
        assertThat(response.myReservation().status()).isEqualTo(ReservationStatus.PENDIENTE);
    }

    @Test
    void conReservaAceptadaSeVenPatenteYTelefono() {
        givenMyReservations(ReservationStatus.ACEPTADA);

        TripDTO response = service.getDetail(passenger.getId(), trip.getId());

        assertThat(response.vehiclePlate()).isEqualTo("AB123CD");
        assertThat(response.driverPhone()).isEqualTo("+5491155550000");
    }

    @Test
    void miReservaEsLaActivaAunqueHayaUnaRechazadaMasNueva() {
        givenMyReservations(ReservationStatus.RECHAZADA, ReservationStatus.PENDIENTE);

        TripDTO response = service.getDetail(passenger.getId(), trip.getId());

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

        assertThatThrownBy(() -> service.getDetail(passenger.getId(), unknown)).isInstanceOf(ApiException.class);
    }
}

package com.example.uniride.backend.trip;

import com.example.uniride.backend.common.error.ApiException;
import com.example.uniride.backend.reservation.ReservationRepository;
import com.example.uniride.backend.reservation.ReservationStatus;
import com.example.uniride.backend.trip.dto.TripResponse;
import com.example.uniride.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

/** RNF07: plate and phone of the driver only with an accepted reservation. */
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
        service = new TripService(tripRepository, reservationRepository);
        driver = driver();
        passenger = passenger();
        trip = trip(driver, 3);
        lenient().when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
    }

    private void givenMyReservations(ReservationStatus... statuses) {
        var list = java.util.Arrays.stream(statuses).map(s -> reservation(trip, passenger, s)).toList();
        when(reservationRepository.findByTripIdAndPassengerIdOrderByReservedAtDesc(trip.getId(), passenger.getId()))
                .thenReturn(list);
    }

    @Test
    void sinReservaNoSeVenPatenteNiTelefono() {
        givenMyReservations();

        TripResponse response = service.getDetail(passenger.getId(), trip.getId());

        assertThat(response.vehiclePlate()).isNull();
        assertThat(response.driverPhone()).isNull();
        assertThat(response.myReservation()).isNull();
        assertThat(response.driverName()).isEqualTo("Lucía F.");
    }

    @Test
    void conReservaPendienteNoSeVenPatenteNiTelefono() {
        givenMyReservations(ReservationStatus.PENDIENTE);

        TripResponse response = service.getDetail(passenger.getId(), trip.getId());

        assertThat(response.vehiclePlate()).isNull();
        assertThat(response.driverPhone()).isNull();
        assertThat(response.myReservation().status()).isEqualTo(ReservationStatus.PENDIENTE);
    }

    @Test
    void conReservaAceptadaSeVenPatenteYTelefono() {
        givenMyReservations(ReservationStatus.ACEPTADA);

        TripResponse response = service.getDetail(passenger.getId(), trip.getId());

        assertThat(response.vehiclePlate()).isEqualTo("AB123CD");
        assertThat(response.driverPhone()).isEqualTo("+5491155550000");
    }

    @Test
    void elConductorVeSusPropiosDatos() {
        when(reservationRepository.findByTripIdAndPassengerIdOrderByReservedAtDesc(trip.getId(), driver.getId()))
                .thenReturn(List.of());

        TripResponse response = service.getDetail(driver.getId(), trip.getId());

        assertThat(response.vehiclePlate()).isEqualTo("AB123CD");
        assertThat(response.driverPhone()).isEqualTo("+5491155550000");
    }

    @Test
    void miReservaEsLaActivaAunqueHayaUnaRechazadaMasNueva() {
        givenMyReservations(ReservationStatus.RECHAZADA, ReservationStatus.PENDIENTE);

        TripResponse response = service.getDetail(passenger.getId(), trip.getId());

        assertThat(response.myReservation().status()).isEqualTo(ReservationStatus.PENDIENTE);
    }

    @Test
    void viajeInexistenteDevuelveNotFound() {
        UUID unknown = UUID.randomUUID();
        when(tripRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(passenger.getId(), unknown)).isInstanceOf(ApiException.class);
    }
}

package com.example.uniride.backend.service;

import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.example.uniride.backend.TestData.driver;
import static com.example.uniride.backend.TestData.passenger;
import static com.example.uniride.backend.TestData.reservation;
import static com.example.uniride.backend.TestData.trip;
import static org.assertj.core.api.Assertions.assertThat;

/** RNF07: plate and phones only between the driver and accepted passengers. */
class ContactVisibilityPolicyTest {

    private final ContactVisibilityPolicy policy = new ContactVisibilityPolicy();
    private final User driver = driver();
    private final User passenger = passenger();
    private final Trip trip = trip(driver, 3);

    @Test
    void elConductorVeSusPropiosDatos() {
        assertThat(policy.canSeeDriverContact(trip, driver.getId(), null)).isTrue();
    }

    @Test
    void sinReservaNoSeVenLosDatosDelConductor() {
        assertThat(policy.canSeeDriverContact(trip, passenger.getId(), null)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"PENDIENTE", "RECHAZADA", "CANCELADA"})
    void conUnaReservaQueNoEstaAceptadaNoSeVenLosDatosDelConductor(ReservationStatus status) {
        Reservation mine = reservation(trip, passenger, status);

        assertThat(policy.canSeeDriverContact(trip, passenger.getId(), mine)).isFalse();
    }

    @Test
    void conReservaAceptadaSeVenLosDatosDelConductor() {
        Reservation mine = reservation(trip, passenger, ReservationStatus.ACEPTADA);

        assertThat(policy.canSeeDriverContact(trip, passenger.getId(), mine)).isTrue();
    }

    @Test
    void laReservaAceptadaDeOtroNoAlcanza() {
        Reservation someoneElses = reservation(trip, passenger(), ReservationStatus.ACEPTADA);

        assertThat(policy.canSeeDriverContact(trip, passenger.getId(), someoneElses)).isFalse();
    }

    @Test
    void elConductorNoVeElTelefonoDeUnaSolicitudPendiente() {
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);

        assertThat(policy.canSeePassengerContact(pending, driver.getId())).isFalse();
    }

    @Test
    void elConductorVeElTelefonoDeUnaReservaAceptada() {
        Reservation accepted = reservation(trip, passenger, ReservationStatus.ACEPTADA);

        assertThat(policy.canSeePassengerContact(accepted, driver.getId())).isTrue();
    }

    @Test
    void otroPasajeroNoVeElTelefonoDeUnaReservaAceptada() {
        Reservation accepted = reservation(trip, passenger, ReservationStatus.ACEPTADA);

        assertThat(policy.canSeePassengerContact(accepted, passenger().getId())).isFalse();
    }
}

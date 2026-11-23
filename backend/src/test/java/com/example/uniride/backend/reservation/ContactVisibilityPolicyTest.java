package com.example.uniride.backend.reservation;

import com.example.uniride.backend.trip.Trip;
import com.example.uniride.backend.user.User;
import org.junit.jupiter.api.Test;

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
    void sinReservaOConReservaNoAceptadaNoSeVenLosDatosDelConductor() {
        assertThat(policy.canSeeDriverContact(trip, passenger.getId(), null)).isFalse();
        for (ReservationStatus status : new ReservationStatus[]{
                ReservationStatus.PENDIENTE, ReservationStatus.RECHAZADA, ReservationStatus.CANCELADA}) {
            Reservation mine = reservation(trip, passenger, status);
            assertThat(policy.canSeeDriverContact(trip, passenger.getId(), mine)).as(status.name()).isFalse();
        }
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
    void elTelefonoDelPasajeroSoloLoVeElConductorConLaReservaAceptada() {
        Reservation pending = reservation(trip, passenger, ReservationStatus.PENDIENTE);
        Reservation accepted = reservation(trip, passenger(), ReservationStatus.ACEPTADA);

        assertThat(policy.canSeePassengerContact(pending, driver.getId())).isFalse();
        assertThat(policy.canSeePassengerContact(accepted, driver.getId())).isTrue();
        assertThat(policy.canSeePassengerContact(accepted, accepted.getPassenger().getId())).isFalse();
    }
}

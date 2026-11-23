package com.example.uniride.backend.reservation;

import com.example.uniride.backend.trip.Trip;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RNF07: the plate and the phones are only shared between people who travel together, that
 * is, the driver and a passenger with an {@code ACEPTADA} reservation. Every response that
 * includes contact data asks this policy; nobody else decides it.
 */
@Component
public class ContactVisibilityPolicy {

    /**
     * Plate of the vehicle and phone of the driver.
     *
     * @param myReservation reservation of the viewer on the trip, or {@code null}
     */
    public boolean canSeeDriverContact(Trip trip, UUID viewerId, Reservation myReservation) {
        if (trip.isDrivenBy(viewerId)) {
            return true;
        }
        return myReservation != null
                && myReservation.belongsTo(viewerId)
                && myReservation.isFor(trip)
                && myReservation.getStatus() == ReservationStatus.ACEPTADA;
    }

    /** Phone of the passenger: only for the driver of the trip, once the reservation is accepted. */
    public boolean canSeePassengerContact(Reservation reservation, UUID viewerId) {
        return reservation.getTrip().isDrivenBy(viewerId)
                && reservation.getStatus() == ReservationStatus.ACEPTADA;
    }
}

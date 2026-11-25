package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.ReservationResponseDTO;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.Trip;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps entities to the DTOs that a given user can see. It is the only place that asks
 * {@link ContactVisibilityPolicy} (RNF07), so every endpoint applies the rule the same way:
 * RF02 and RF03 map their trips with {@link #trip} too.
 */
@Component
public class ViewMapper {

    private final ContactVisibilityPolicy contactVisibility;

    public ViewMapper(ContactVisibilityPolicy contactVisibility) {
        this.contactVisibility = contactVisibility;
    }

    public ReservationResponseDTO reservation(Reservation reservation, UUID viewerId) {
        return ReservationResponseDTO.from(reservation, contactVisibility.canSeePassengerContact(reservation, viewerId));
    }

    /** @param myReservation reservation of the viewer on the trip, or {@code null} */
    public TripResponseDTO trip(Trip trip, UUID viewerId, Reservation myReservation) {
        ReservationResponseDTO mine = myReservation == null ? null : reservation(myReservation, viewerId);
        return TripResponseDTO.from(trip, mine, contactVisibility.canSeeDriverContact(trip, viewerId, myReservation));
    }
}

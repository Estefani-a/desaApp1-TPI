package com.example.uniride.backend.trip;

import com.example.uniride.backend.common.error.ApiException;
import com.example.uniride.backend.common.error.ErrorCode;
import com.example.uniride.backend.reservation.ContactVisibilityPolicy;
import com.example.uniride.backend.reservation.Reservation;
import com.example.uniride.backend.reservation.ReservationRepository;
import com.example.uniride.backend.reservation.dto.ReservationResponse;
import com.example.uniride.backend.trip.dto.TripResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Trips (docs/API.md §5). RF02 and RF03 add publishing and the list here. */
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;
    private final ContactVisibilityPolicy contactVisibility;

    public TripService(TripRepository tripRepository, ReservationRepository reservationRepository,
                       ContactVisibilityPolicy contactVisibility) {
        this.tripRepository = tripRepository;
        this.reservationRepository = reservationRepository;
        this.contactVisibility = contactVisibility;
    }

    /** {@code GET /trips/{tripId}}: plate and phone only for the driver or an accepted passenger. */
    @Transactional(readOnly = true)
    public TripResponse getDetail(UUID viewerId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        return toResponse(trip, viewerId, findMyReservation(tripId, viewerId));
    }

    /**
     * Maps a trip for [viewerId]. RF02 and RF03 use it too, so the RNF07 rule is applied the
     * same way in every response.
     */
    TripResponse toResponse(Trip trip, UUID viewerId, Reservation myReservation) {
        ReservationResponse mine = myReservation == null ? null
                : ReservationResponse.from(myReservation, contactVisibility.canSeePassengerContact(myReservation, viewerId));
        return TripResponse.from(trip, mine, contactVisibility.canSeeDriverContact(trip, viewerId, myReservation));
    }

    /**
     * Reservation of the viewer on the trip: the active one if there is one, otherwise the
     * newest (for example, a rejected request). {@code null} if the viewer never asked.
     */
    private Reservation findMyReservation(UUID tripId, UUID viewerId) {
        List<Reservation> mine = reservationRepository.findByTripIdAndPassengerIdOrderByReservedAtDesc(tripId, viewerId);
        return mine.stream()
                .filter(reservation -> reservation.getStatus().isActive())
                .findFirst()
                .orElse(mine.isEmpty() ? null : mine.getFirst());
    }
}

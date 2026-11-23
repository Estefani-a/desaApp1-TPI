package com.example.uniride.backend.trip;

import com.example.uniride.backend.common.error.ApiException;
import com.example.uniride.backend.common.error.ErrorCode;
import com.example.uniride.backend.reservation.Reservation;
import com.example.uniride.backend.reservation.ReservationRepository;
import com.example.uniride.backend.reservation.ReservationStatus;
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

    public TripService(TripRepository tripRepository, ReservationRepository reservationRepository) {
        this.tripRepository = tripRepository;
        this.reservationRepository = reservationRepository;
    }

    /** {@code GET /trips/{tripId}}: plate and phone only for the driver or an accepted passenger. */
    @Transactional(readOnly = true)
    public TripResponse getDetail(UUID viewerId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        return TripResponse.from(trip, viewerId, findMyReservation(tripId, viewerId));
    }

    /**
     * Reservation of the viewer on the trip: the active one if there is one, otherwise the
     * newest (for example, a rejected request). {@code null} if the viewer never asked.
     */
    Reservation findMyReservation(UUID tripId, UUID viewerId) {
        List<Reservation> mine = reservationRepository.findByTripIdAndPassengerIdOrderByReservedAtDesc(tripId, viewerId);
        return mine.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDIENTE || r.getStatus() == ReservationStatus.ACEPTADA)
                .findFirst()
                .orElse(mine.isEmpty() ? null : mine.getFirst());
    }
}

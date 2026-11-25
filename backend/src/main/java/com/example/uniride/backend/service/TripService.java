package com.example.uniride.backend.service;

import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.dto.TripDTO;
import com.example.uniride.backend.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Trips (docs/API.md §5). RF02 and RF03 add publishing and the list here. */
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;
    private final ViewMapper viewMapper;

    public TripService(TripRepository tripRepository, ReservationRepository reservationRepository,
                       ViewMapper viewMapper) {
        this.tripRepository = tripRepository;
        this.reservationRepository = reservationRepository;
        this.viewMapper = viewMapper;
    }

    /** {@code GET /trips/{tripId}}: plate and phone only for the driver or an accepted passenger. */
    @Transactional(readOnly = true)
    public TripDTO getDetail(UUID viewerId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        return viewMapper.trip(trip, viewerId, findMyReservation(tripId, viewerId));
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

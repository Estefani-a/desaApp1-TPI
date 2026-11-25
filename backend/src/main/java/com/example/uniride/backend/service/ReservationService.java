package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.ReservationDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.repository.TripRepository;
import com.example.uniride.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Seat requests (RF04, docs/API.md §6). This service finds, locks and checks who is asking;
 * the rules of each transition and of the seats live in {@link Reservation} and {@link Trip}.
 *
 * <p>Every write reads the trip with a row lock ({@link TripRepository#findByIdForUpdate}), so
 * everything that touches the same trip runs one after the other: two acceptances never leave
 * negative seats, and a retry of the same request never collides with the first one.
 */
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ViewMapper viewMapper;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository, TripRepository tripRepository,
                              UserRepository userRepository, ViewMapper viewMapper, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.viewMapper = viewMapper;
        this.clock = clock;
    }

    /**
     * {@code PUT /trips/{tripId}/reservations/{reservationId}}. Creates the reservation in
     * {@code PENDIENTE}; it does not take a seat (that happens when the driver accepts).
     */
    @Transactional
    public RequestSeatResult requestSeat(UUID userId, UUID tripId, UUID reservationId) {
        Trip trip = lockTrip(tripId);

        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        if (existing.isPresent()) {
            Reservation reservation = existing.get();
            if (!reservation.belongsTo(userId) || !reservation.isFor(trip)) {
                throw new ApiException(ErrorCode.ID_CONFLICT);
            }
            return new RequestSeatResult(viewMapper.reservation(reservation, userId), false);
        }

        if (trip.isDrivenBy(userId)) {
            throw new ApiException(ErrorCode.CANNOT_RESERVE_OWN_TRIP);
        }
        // Before the state of the trip: whoever already has a seat gets ALREADY_RESERVED even if
        // their own acceptance filled the trip.
        if (reservationRepository.existsByTripIdAndPassengerIdAndStatusIn(tripId, userId, ReservationStatus.ACTIVE)) {
            throw new ApiException(ErrorCode.ALREADY_RESERVED);
        }
        trip.requireOpenForRequests(clock.instant());
        User passenger = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

        Reservation created = saveNew(new Reservation(reservationId, trip, passenger, clock.instant()));
        // TODO(push, D5): enviar NUEVA_SOLICITUD al conductor (docs/API.md §9)
        return new RequestSeatResult(viewMapper.reservation(created, userId), true);
    }

    /** {@code GET /trips/{tripId}/reservations}: only for the driver. */
    @Transactional(readOnly = true)
    public List<ReservationDTO> listForTrip(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        requireDriver(trip, userId);
        return reservationRepository.findByTripIdOrderByReservedAtAsc(tripId).stream()
                .map(reservation -> viewMapper.reservation(reservation, userId))
                .toList();
    }

    /** The driver accepts: takes a seat; the trip becomes {@code COMPLETO} at 0. */
    @Transactional
    public ReservationDTO accept(UUID userId, UUID tripId, UUID reservationId) {
        Reservation reservation = findForDriver(userId, tripId, reservationId);
        reservation.accept(clock.instant());
        // TODO(push, D5): enviar SOLICITUD_ACEPTADA al pasajero (docs/API.md §9)
        return viewMapper.reservation(reservation, userId);
    }

    /** The driver rejects. Does not touch the seats. */
    @Transactional
    public ReservationDTO reject(UUID userId, UUID tripId, UUID reservationId) {
        Reservation reservation = findForDriver(userId, tripId, reservationId);
        reservation.reject(clock.instant());
        // TODO(push, D5): enviar SOLICITUD_RECHAZADA al pasajero (docs/API.md §9)
        return viewMapper.reservation(reservation, userId);
    }

    /** The passenger cancels. An accepted reservation gives the seat back. */
    @Transactional
    public ReservationDTO cancel(UUID userId, UUID tripId, UUID reservationId) {
        Reservation reservation = findReservation(lockTrip(tripId), reservationId);
        if (!reservation.belongsTo(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        reservation.cancel(clock.instant());
        return viewMapper.reservation(reservation, userId);
    }

    /**
     * Inserts the reservation. The database index is the last guard of "one active reservation
     * per passenger" (the lock already prevents it): if it trips, the answer is the same as the
     * check above.
     */
    private Reservation saveNew(Reservation reservation) {
        try {
            return reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException ex) {
            String detail = String.valueOf(ex.getMostSpecificCause().getMessage());
            if (detail.contains(Reservation.ONE_ACTIVE_PER_PASSENGER_INDEX)) {
                throw new ApiException(ErrorCode.ALREADY_RESERVED);
            }
            throw ex;
        }
    }

    private Reservation findForDriver(UUID userId, UUID tripId, UUID reservationId) {
        Trip trip = lockTrip(tripId);
        requireDriver(trip, userId);
        return findReservation(trip, reservationId);
    }

    private Trip lockTrip(UUID tripId) {
        return tripRepository.findByIdForUpdate(tripId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    /** The reservation has to exist and belong to the trip of the path. */
    private Reservation findReservation(Trip trip, UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .filter(reservation -> reservation.isFor(trip))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private void requireDriver(Trip trip, UUID userId) {
        if (!trip.isDrivenBy(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    /** {@code created} decides between 201 (new) and 200 (retry of the same request). */
    public record RequestSeatResult(ReservationDTO reservation, boolean created) {
    }
}

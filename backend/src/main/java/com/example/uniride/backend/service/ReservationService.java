package com.example.uniride.backend.service;

import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.dto.ReservationDTO;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.repository.TripRepository;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Seat requests (RF04, docs/API.md §6). This service finds, locks and checks permissions;
 * the transitions and the seats are rules of {@link Reservation} and {@link Trip}.
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
    private final ContactVisibilityPolicy contactVisibility;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository, TripRepository tripRepository,
                              UserRepository userRepository, ContactVisibilityPolicy contactVisibility, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.contactVisibility = contactVisibility;
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
            return new RequestSeatResult(toResponse(reservation, userId), false);
        }

        if (trip.isDrivenBy(userId)) {
            throw new ApiException(ErrorCode.CANNOT_RESERVE_OWN_TRIP);
        }
        if (!trip.acceptsRequestsAt(now())) {
            throw new ApiException(ErrorCode.TRIP_NOT_OPEN);
        }
        if (reservationRepository.existsByTripIdAndPassengerIdAndStatusIn(tripId, userId, ReservationStatus.ACTIVE)) {
            throw new ApiException(ErrorCode.ALREADY_RESERVED);
        }
        User passenger = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

        Reservation created = reservationRepository.save(new Reservation(reservationId, trip, passenger, clock.instant()));
        // TODO(push, D5): enviar NUEVA_SOLICITUD al conductor (docs/API.md §9)
        return new RequestSeatResult(toResponse(created, userId), true);
    }

    /** {@code GET /trips/{tripId}/reservations}: only for the driver. */
    @Transactional(readOnly = true)
    public List<ReservationDTO> listForTrip(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        requireDriver(trip, userId);
        return reservationRepository.findByTripIdOrderByReservedAtAsc(tripId).stream()
                .map(reservation -> toResponse(reservation, userId))
                .toList();
    }

    /** The driver accepts: takes a seat; the trip becomes {@code COMPLETO} at 0. */
    @Transactional
    public ReservationDTO accept(UUID userId, UUID tripId, UUID reservationId) {
        // TODO(push, D5): enviar SOLICITUD_ACEPTADA al pasajero (docs/API.md §9)
        return asDriver(userId, tripId, reservationId, Reservation::accept, ReservationStatus.ACEPTADA);
    }

    /** The driver rejects. Does not touch the seats. */
    @Transactional
    public ReservationDTO reject(UUID userId, UUID tripId, UUID reservationId) {
        // TODO(push, D5): enviar SOLICITUD_RECHAZADA al pasajero (docs/API.md §9)
        return asDriver(userId, tripId, reservationId, Reservation::reject, ReservationStatus.RECHAZADA);
    }

    /** The passenger cancels. An accepted reservation gives the seat back. */
    @Transactional
    public ReservationDTO cancel(UUID userId, UUID tripId, UUID reservationId) {
        Trip trip = lockTrip(tripId);
        Reservation reservation = findReservation(trip, reservationId);
        if (!reservation.belongsTo(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        requireNotDeparted(trip, reservation, ReservationStatus.CANCELADA);
        reservation.cancel(clock.instant());
        return toResponse(reservation, userId);
    }

    private ReservationDTO asDriver(UUID userId, UUID tripId, UUID reservationId,
                                         BiConsumer<Reservation, Instant> transition, ReservationStatus target) {
        Trip trip = lockTrip(tripId);
        requireDriver(trip, userId);
        Reservation reservation = findReservation(trip, reservationId);
        requireNotDeparted(trip, reservation, target);
        transition.accept(reservation, clock.instant());
        return toResponse(reservation, userId);
    }

    /**
     * Once the trip left, its reservations do not change anymore. A repeated transition that
     * already happened is still answered (idempotency), so a reservation that is already in
     * [target] passes.
     */
    private void requireNotDeparted(Trip trip, Reservation reservation, ReservationStatus target) {
        if (trip.hasDepartedAt(now()) && reservation.getStatus() != target) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
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

    private ReservationDTO toResponse(Reservation reservation, UUID viewerId) {
        return ReservationDTO.from(reservation, contactVisibility.canSeePassengerContact(reservation, viewerId));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** {@code created} decides between 201 (new) and 200 (retry of the same request). */
    public record RequestSeatResult(ReservationDTO reservation, boolean created) {
    }
}

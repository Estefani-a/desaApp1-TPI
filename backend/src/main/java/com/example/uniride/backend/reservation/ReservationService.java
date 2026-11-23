package com.example.uniride.backend.reservation;

import com.example.uniride.backend.common.error.ApiException;
import com.example.uniride.backend.common.error.ErrorCode;
import com.example.uniride.backend.reservation.dto.ReservationResponse;
import com.example.uniride.backend.trip.Trip;
import com.example.uniride.backend.trip.TripRepository;
import com.example.uniride.backend.trip.TripStatus;
import com.example.uniride.backend.user.User;
import com.example.uniride.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Seat requests (RF04, docs/API.md §6). Every transition is idempotent: repeating it on a
 * reservation that is already in the target state answers the reservation as it is.
 *
 * <p>Accepting and cancelling change the seats of the trip, so they read it with a row lock
 * ({@link TripRepository#findByIdForUpdate}): two simultaneous acceptances never leave
 * negative seats.
 */
@Service
public class ReservationService {

    static final Set<ReservationStatus> ACTIVE = EnumSet.of(ReservationStatus.PENDIENTE, ReservationStatus.ACEPTADA);

    private final ReservationRepository reservationRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository, TripRepository tripRepository,
                              UserRepository userRepository, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * {@code PUT /trips/{tripId}/reservations/{reservationId}}. Creates the reservation in
     * {@code PENDIENTE}; it does not take a seat (that happens when the driver accepts).
     */
    @Transactional
    public RequestSeatResult requestSeat(UUID userId, UUID tripId, UUID reservationId) {
        var existing = reservationRepository.findById(reservationId);
        if (existing.isPresent()) {
            Reservation reservation = existing.get();
            if (!reservation.belongsTo(userId) || !reservation.getTrip().getId().equals(tripId)) {
                throw new ApiException(ErrorCode.ID_CONFLICT);
            }
            return new RequestSeatResult(ReservationResponse.from(reservation, userId), false);
        }

        Trip trip = findTrip(tripId);
        if (trip.isDrivenBy(userId)) {
            throw new ApiException(ErrorCode.CANNOT_RESERVE_OWN_TRIP);
        }
        if (trip.getStatus() != TripStatus.ABIERTO || hasDeparted(trip)) {
            throw new ApiException(ErrorCode.TRIP_NOT_OPEN);
        }
        if (trip.getAvailableSeats() <= 0) {
            throw new ApiException(ErrorCode.NO_SEATS_AVAILABLE);
        }
        if (reservationRepository.existsByTripIdAndPassengerIdAndStatusIn(tripId, userId, ACTIVE)) {
            throw new ApiException(ErrorCode.ALREADY_RESERVED);
        }
        User passenger = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

        Reservation saved = reservationRepository.saveAndFlush(new Reservation(reservationId, trip, passenger));
        // TODO(push, D5): enviar NUEVA_SOLICITUD al conductor (docs/API.md §9)
        return new RequestSeatResult(ReservationResponse.from(saved, userId), true);
    }

    /** {@code GET /trips/{tripId}/reservations}: only for the driver. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForTrip(UUID userId, UUID tripId) {
        Trip trip = findTrip(tripId);
        requireDriver(trip, userId);
        return reservationRepository.findByTripIdOrderByReservedAtAsc(tripId).stream()
                .map(reservation -> ReservationResponse.from(reservation, userId))
                .toList();
    }

    /**
     * {@code PENDIENTE} → {@code ACEPTADA}. Takes one seat in the same transaction, with the
     * trip row locked; the trip becomes {@code COMPLETO} at 0 seats.
     */
    @Transactional
    public ReservationResponse accept(UUID userId, UUID tripId, UUID reservationId) {
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        requireDriver(trip, userId);
        Reservation reservation = findReservation(trip, reservationId);

        if (reservation.getStatus() == ReservationStatus.ACEPTADA) {
            return ReservationResponse.from(reservation, userId);
        }
        if (reservation.getStatus() != ReservationStatus.PENDIENTE
                || trip.getStatus() == TripStatus.FINALIZADO || trip.getStatus() == TripStatus.CANCELADO) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (trip.getAvailableSeats() <= 0) {
            throw new ApiException(ErrorCode.NO_SEATS_AVAILABLE);
        }

        trip.setAvailableSeats(trip.getAvailableSeats() - 1);
        if (trip.getAvailableSeats() == 0) {
            trip.setStatus(TripStatus.COMPLETO);
        }
        reservation.setStatus(ReservationStatus.ACEPTADA);
        reservationRepository.flush();
        // TODO(push, D5): enviar SOLICITUD_ACEPTADA al pasajero (docs/API.md §9)
        return ReservationResponse.from(reservation, userId);
    }

    /** {@code PENDIENTE} → {@code RECHAZADA}. Does not touch the seats. */
    @Transactional
    public ReservationResponse reject(UUID userId, UUID tripId, UUID reservationId) {
        Trip trip = findTrip(tripId);
        requireDriver(trip, userId);
        Reservation reservation = findReservation(trip, reservationId);

        if (reservation.getStatus() == ReservationStatus.RECHAZADA) {
            return ReservationResponse.from(reservation, userId);
        }
        if (reservation.getStatus() != ReservationStatus.PENDIENTE) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        reservation.setStatus(ReservationStatus.RECHAZADA);
        reservationRepository.flush();
        // TODO(push, D5): enviar SOLICITUD_RECHAZADA al pasajero (docs/API.md §9)
        return ReservationResponse.from(reservation, userId);
    }

    /**
     * The passenger cancels: {@code PENDIENTE} or {@code ACEPTADA} → {@code CANCELADA}. If it
     * was accepted, the seat goes back and a {@code COMPLETO} trip is {@code ABIERTO} again.
     */
    @Transactional
    public ReservationResponse cancel(UUID userId, UUID tripId, UUID reservationId) {
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        Reservation reservation = findReservation(trip, reservationId);
        if (!reservation.belongsTo(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        if (reservation.getStatus() == ReservationStatus.CANCELADA) {
            return ReservationResponse.from(reservation, userId);
        }
        if (!ACTIVE.contains(reservation.getStatus()) || trip.getStatus() == TripStatus.FINALIZADO) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        if (reservation.getStatus() == ReservationStatus.ACEPTADA) {
            trip.setAvailableSeats(Math.min(trip.getAvailableSeats() + 1, trip.getTotalSeats()));
            if (trip.getStatus() == TripStatus.COMPLETO) {
                trip.setStatus(TripStatus.ABIERTO);
            }
        }
        reservation.setStatus(ReservationStatus.CANCELADA);
        reservationRepository.flush();
        return ReservationResponse.from(reservation, userId);
    }

    private Trip findTrip(UUID tripId) {
        return tripRepository.findById(tripId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    /** The reservation has to exist and belong to the trip of the path. */
    private Reservation findReservation(Trip trip, UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .filter(reservation -> reservation.getTrip().getId().equals(trip.getId()))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private void requireDriver(Trip trip, UUID userId) {
        if (!trip.isDrivenBy(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean hasDeparted(Trip trip) {
        return LocalDateTime.of(trip.getDepartureDate(), trip.getDepartureTime())
                .isBefore(LocalDateTime.now(clock));
    }

    /** {@code created} decides between 201 (new) and 200 (retry of the same request). */
    public record RequestSeatResult(ReservationResponse reservation, boolean created) {
    }
}

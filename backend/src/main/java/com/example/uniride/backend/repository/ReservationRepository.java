package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    /** Requests of a trip, oldest first (driver panel), with the passenger in the same query. */
    @EntityGraph(attributePaths = "passenger")
    List<Reservation> findByTripIdOrderByReservedAtAsc(UUID tripId);

    /**
     * Reservations of a passenger on several trips at once, for the {@code myReservation} of
     * each trip of the list (RF03) without one query per trip.
     */
    @EntityGraph(attributePaths = "passenger")
    List<Reservation> findByPassengerIdAndTripIdIn(UUID passengerId, Collection<UUID> tripIds);

    /** Reservations of a passenger on a trip, newest first. */
    List<Reservation> findByTripIdAndPassengerIdOrderByReservedAtDesc(UUID tripId, UUID passengerId);

    boolean existsByTripIdAndPassengerIdAndStatusIn(UUID tripId, UUID passengerId,
                                                    Collection<ReservationStatus> statuses);
}

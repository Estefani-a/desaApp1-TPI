package com.example.uniride.backend.reservation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    /** Requests of a trip, oldest first (driver panel), with the passenger in the same query. */
    @EntityGraph(attributePaths = "passenger")
    List<Reservation> findByTripIdOrderByReservedAtAsc(UUID tripId);

    /** Reservations of a passenger on a trip, newest first. */
    List<Reservation> findByTripIdAndPassengerIdOrderByReservedAtDesc(UUID tripId, UUID passengerId);

    boolean existsByTripIdAndPassengerIdAndStatusIn(UUID tripId, UUID passengerId,
                                                    Collection<ReservationStatus> statuses);
}

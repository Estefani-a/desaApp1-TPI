package com.example.uniride.backend.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** RF04 adds the queries it needs. */
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
}

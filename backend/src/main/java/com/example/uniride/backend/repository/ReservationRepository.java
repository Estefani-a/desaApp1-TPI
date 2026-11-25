package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** RF04 adds the queries it needs. */
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
}

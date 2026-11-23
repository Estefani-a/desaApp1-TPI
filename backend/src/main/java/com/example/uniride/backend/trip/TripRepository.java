package com.example.uniride.backend.trip;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Each RF adds the queries it needs (filters of RF03, row lock of RF04…). */
public interface TripRepository extends JpaRepository<Trip, UUID> {

    /**
     * Reads the trip with {@code SELECT ... FOR UPDATE}: two transactions that change the
     * seats of the same trip run one after the other (RF04, CONVENCIONES §10.2).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") UUID id);
}

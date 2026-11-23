package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Each RF adds the queries it needs (filters of RF03, row lock of RF04…). */
public interface TripRepository extends JpaRepository<Trip, UUID> {
}

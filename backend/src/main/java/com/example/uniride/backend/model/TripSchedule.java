package com.example.uniride.backend.model;

import com.example.uniride.backend.util.BusinessTime;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/** When the trip leaves and, optionally, when it is expected to arrive (business time zone). */
@Embeddable
public class TripSchedule {

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "estimated_arrival_time")
    private LocalTime estimatedArrivalTime;

    protected TripSchedule() {
    }

    public TripSchedule(LocalDate departureDate, LocalTime departureTime, LocalTime estimatedArrivalTime) {
        this.departureDate = Objects.requireNonNull(departureDate);
        this.departureTime = Objects.requireNonNull(departureTime);
        this.estimatedArrivalTime = estimatedArrivalTime;
    }

    public boolean hasDepartedAt(Instant now) {
        return LocalDateTime.of(departureDate, departureTime).isBefore(LocalDateTime.ofInstant(now, BusinessTime.ZONE));
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public LocalTime getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }
}

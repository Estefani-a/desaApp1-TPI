package com.example.uniride.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

/** Where the trip goes: direction, meeting point at the campus and destination. */
@Embeddable
public class TripRoute {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Column(name = "meeting_point", nullable = false, length = 100)
    private String meetingPoint;

    @Column(name = "destination_neighborhood", nullable = false, length = 100)
    private String destinationNeighborhood;

    @Column(name = "destination_reference", length = 150)
    private String destinationReference;

    protected TripRoute() {
    }

    public TripRoute(Direction direction, String meetingPoint, String destinationNeighborhood,
                     String destinationReference) {
        this.direction = Objects.requireNonNull(direction);
        this.meetingPoint = Objects.requireNonNull(meetingPoint);
        this.destinationNeighborhood = Objects.requireNonNull(destinationNeighborhood);
        this.destinationReference = destinationReference;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getMeetingPoint() {
        return meetingPoint;
    }

    public String getDestinationNeighborhood() {
        return destinationNeighborhood;
    }

    public String getDestinationReference() {
        return destinationReference;
    }
}

package com.example.uniride.backend.dto;

import com.example.uniride.backend.service.ContactVisibilityPolicy;
import com.example.uniride.backend.model.Direction;
import com.example.uniride.backend.util.PublicName;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.model.TripStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * {@code TripDto} of docs/API.md §3.3. Shared by every endpoint that returns a trip.
 * {@code vehiclePlate} and {@code driverPhone} are {@code null} unless the caller decided,
 * with {@code ContactVisibilityPolicy}, that the viewer can see them (RNF07).
 */
public record TripDTO(
        UUID tripId,
        UUID driverId,
        String driverName,
        Double driverRating,
        Direction direction,
        String campus,
        String meetingPoint,
        String destinationNeighborhood,
        String destinationReference,
        LocalDate departureDate,
        LocalTime departureTime,
        LocalTime estimatedArrivalTime,
        int totalSeats,
        int availableSeats,
        int contributionPerSeat,
        String vehicleModel,
        String vehicleColor,
        String vehiclePlate,
        String driverPhone,
        TripStatus status,
        ReservationDTO myReservation,
        Instant updatedAt
) {

    /**
     * @param myReservation reservation of the viewer, already mapped, or {@code null}
     * @param showContact   decided by {@code ContactVisibilityPolicy}; this record only copies values
     */
    public static TripDTO from(Trip trip, ReservationDTO myReservation, boolean showContact) {
        return new TripDTO(
                trip.getId(),
                trip.getDriver().getId(),
                PublicName.of(trip.getDriver().getFullName()),
                trip.getDriver().getRatingAvg(),
                trip.getRoute().getDirection(),
                trip.getCampus().getCode(),
                trip.getRoute().getMeetingPoint(),
                trip.getRoute().getDestinationNeighborhood(),
                trip.getRoute().getDestinationReference(),
                trip.getSchedule().getDepartureDate(),
                trip.getSchedule().getDepartureTime(),
                trip.getSchedule().getEstimatedArrivalTime(),
                trip.getTotalSeats(),
                trip.getAvailableSeats(),
                trip.getContributionPerSeat(),
                trip.getVehicle().getModel(),
                trip.getVehicle().getColor(),
                showContact ? trip.getVehicle().getPlate() : null,
                showContact ? trip.getDriver().getPhone() : null,
                trip.getStatus(),
                myReservation,
                trip.getUpdatedAt());
    }
}

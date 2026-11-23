package com.example.uniride.backend.trip.dto;

import com.example.uniride.backend.reservation.dto.ReservationResponse;
import com.example.uniride.backend.trip.Direction;
import com.example.uniride.backend.trip.Trip;
import com.example.uniride.backend.trip.TripStatus;
import com.example.uniride.backend.user.PublicName;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * {@code TripDto} of docs/API.md §3.3. Shared by every endpoint that returns a trip.
 * {@code vehiclePlate} and {@code driverPhone} are {@code null} unless the caller decided,
 * with {@code ContactVisibilityPolicy}, that the viewer can see them (RNF07).
 */
public record TripResponse(
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
        ReservationResponse myReservation,
        Instant updatedAt
) {

    /**
     * @param myReservation reservation of the viewer, already mapped, or {@code null}
     * @param showContact   decided by {@code ContactVisibilityPolicy}; this record only copies values
     */
    public static TripResponse from(Trip trip, ReservationResponse myReservation, boolean showContact) {
        return new TripResponse(
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

package com.example.uniride.backend.trip.dto;

import com.example.uniride.backend.reservation.Reservation;
import com.example.uniride.backend.reservation.ReservationStatus;
import com.example.uniride.backend.reservation.dto.ReservationResponse;
import com.example.uniride.backend.trip.Direction;
import com.example.uniride.backend.trip.Trip;
import com.example.uniride.backend.trip.TripStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * {@code TripDto} of docs/API.md §3.3. Shared by every endpoint that returns a trip.
 *
 * <p>RNF07: {@code vehiclePlate} and {@code driverPhone} only have a value when the viewer is
 * the driver or has an {@code ACEPTADA} reservation on the trip. In any other case they are
 * {@code null}.
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
     * @param viewerId      user that makes the request
     * @param myReservation reservation of the viewer on this trip, or {@code null}
     */
    public static TripResponse from(Trip trip, UUID viewerId, Reservation myReservation) {
        boolean showContact = trip.isDrivenBy(viewerId)
                || (myReservation != null && myReservation.getStatus() == ReservationStatus.ACEPTADA);
        return new TripResponse(
                trip.getId(),
                trip.getDriver().getId(),
                trip.getDriver().getPublicName(),
                trip.getDriver().getRatingAvg(),
                trip.getDirection(),
                trip.getCampus().getCode(),
                trip.getMeetingPoint(),
                trip.getDestinationNeighborhood(),
                trip.getDestinationReference(),
                trip.getDepartureDate(),
                trip.getDepartureTime(),
                trip.getEstimatedArrivalTime(),
                trip.getTotalSeats(),
                trip.getAvailableSeats(),
                trip.getContributionPerSeat(),
                trip.getVehicleModel(),
                trip.getVehicleColor(),
                showContact ? trip.getVehiclePlate() : null,
                showContact ? trip.getDriver().getPhone() : null,
                trip.getStatus(),
                myReservation == null ? null : ReservationResponse.from(myReservation, viewerId),
                trip.getUpdatedAt());
    }
}

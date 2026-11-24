package com.example.uniride.data.mapper

import com.example.uniride.data.remote.dto.CampusDto
import com.example.uniride.data.remote.dto.TripDto
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.Participant
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripRoute
import com.example.uniride.domain.model.TripSchedule
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.domain.model.TripVehicle
import com.example.uniride.domain.model.UserId
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

/*
 * TripDto (docs/API.md §3.3) ↔ domain. The enums travel with the same names in the API and in
 * the app, so `valueOf` reads them directly; an unknown value is an error of the contract and
 * fails the refresh (safeApiCall turns it into DataError.UNKNOWN).
 */

fun TripDto.toDomain(): Trip = Trip(
    id = TripId(tripId),
    driver = Participant(
        id = UserId(driverId),
        displayName = driverName,
        rating = driverRating,
        phone = driverPhone,
    ),
    campus = CampusCode(campus),
    route = TripRoute(
        direction = Direction.valueOf(direction),
        meetingPoint = meetingPoint,
        destinationNeighborhood = destinationNeighborhood,
        destinationReference = destinationReference,
    ),
    schedule = TripSchedule(
        departureDate = LocalDate.parse(departureDate),
        departureTime = LocalTime.parse(departureTime),
        estimatedArrivalTime = estimatedArrivalTime?.let(LocalTime::parse),
    ),
    totalSeats = totalSeats,
    availableSeats = availableSeats,
    contributionPerSeat = contributionPerSeat,
    vehicle = TripVehicle(model = vehicleModel, color = vehicleColor, plate = vehiclePlate),
    status = TripStatus.valueOf(status),
    updatedAt = OffsetDateTime.parse(updatedAt).toInstant(),
)

fun CampusDto.toDomain(): Campus = Campus(
    code = CampusCode(code),
    name = name,
    meetingPoints = meetingPoints,
)

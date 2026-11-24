package com.example.uniride.data.mapper

import com.example.uniride.data.local.entity.TripEntity
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Participant
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripRoute
import com.example.uniride.domain.model.TripSchedule
import com.example.uniride.domain.model.TripVehicle
import com.example.uniride.domain.model.UserId

fun TripEntity.toDomain(): Trip = Trip(
    id = TripId(tripId),
    driver = Participant(UserId(driverId), driverName, driverRating, driverPhone),
    campus = CampusCode(campus),
    route = TripRoute(direction, meetingPoint, destinationNeighborhood, destinationReference),
    schedule = TripSchedule(departureDate, departureTime, estimatedArrivalTime),
    totalSeats = totalSeats,
    availableSeats = availableSeats,
    contributionPerSeat = contributionPerSeat,
    vehicle = TripVehicle(vehicleModel, vehicleColor, vehiclePlate),
    status = status,
    updatedAt = updatedAt,
)

/** The domain does not know about synchronization, so the caller says whether the server confirmed it. */
fun Trip.toEntity(isSync: Boolean): TripEntity = TripEntity(
    tripId = id.value,
    driverId = driver.id.value,
    driverName = driver.displayName,
    driverRating = driver.rating,
    driverPhone = driver.phone,
    direction = route.direction,
    campus = campus.value,
    meetingPoint = route.meetingPoint,
    destinationNeighborhood = route.destinationNeighborhood,
    destinationReference = route.destinationReference,
    departureDate = schedule.departureDate,
    departureTime = schedule.departureTime,
    estimatedArrivalTime = schedule.estimatedArrivalTime,
    totalSeats = totalSeats,
    availableSeats = availableSeats,
    contributionPerSeat = contributionPerSeat,
    vehicleModel = vehicle.model,
    vehicleColor = vehicle.color,
    vehiclePlate = vehicle.plate,
    status = status,
    isSync = isSync,
    updatedAt = updatedAt,
)

package com.example.uniride.data.mapper

import com.example.uniride.data.local.entity.ReservationEntity
import com.example.uniride.data.remote.dto.ReservationDto
import com.example.uniride.domain.model.Participant
import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationId
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.UserId
import java.time.OffsetDateTime

fun ReservationEntity.toDomain(): Reservation = Reservation(
    id = ReservationId(reservationId),
    tripId = TripId(tripId),
    passenger = Participant(UserId(passengerId), passengerName, passengerRating, passengerPhone),
    status = status,
    reservedAt = reservedAt,
    updatedAt = updatedAt,
)

/** The domain does not know about synchronization, so the caller says whether the server confirmed it. */
fun Reservation.toEntity(isSync: Boolean): ReservationEntity = ReservationEntity(
    reservationId = id.value,
    tripId = tripId.value,
    passengerId = passenger.id.value,
    passengerName = passenger.displayName,
    passengerRating = passenger.rating,
    passengerPhone = passenger.phone,
    status = status,
    reservedAt = reservedAt,
    updatedAt = updatedAt,
    isSync = isSync,
)

/** `ReservationDto` (docs/API.md §3.4) to domain. An unknown status is an error of the contract. */
fun ReservationDto.toDomain(): Reservation = Reservation(
    id = ReservationId(reservationId),
    tripId = TripId(tripId),
    passenger = Participant(UserId(passengerId), passengerName, passengerRating, passengerPhone),
    status = ReservationStatus.valueOf(status),
    reservedAt = OffsetDateTime.parse(reservedAt).toInstant(),
    updatedAt = OffsetDateTime.parse(updatedAt).toInstant(),
)

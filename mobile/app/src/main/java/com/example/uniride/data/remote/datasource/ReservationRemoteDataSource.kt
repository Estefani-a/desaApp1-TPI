package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.dto.ReservationDto

interface ReservationRemoteDataSource {
    suspend fun requestSeat(tripId: String, reservationId: String): ReservationDto
    suspend fun getTripReservations(tripId: String): List<ReservationDto>
    suspend fun acceptReservation(tripId: String, reservationId: String): ReservationDto
    suspend fun rejectReservation(tripId: String, reservationId: String): ReservationDto
    suspend fun cancelReservation(tripId: String, reservationId: String): ReservationDto
}

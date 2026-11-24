package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.api.ReservationApi
import com.example.uniride.data.remote.dto.ReservationDto
import javax.inject.Inject

class RetrofitReservationRemoteDataSource @Inject constructor(
    private val api: ReservationApi,
    private val errorMapper: RetrofitErrorMapper,
) : ReservationRemoteDataSource {

    override suspend fun requestSeat(tripId: String, reservationId: String): ReservationDto =
        errorMapper.execute { api.requestSeat(tripId, reservationId) }

    override suspend fun getTripReservations(tripId: String): List<ReservationDto> =
        errorMapper.execute { api.getTripReservations(tripId) }

    override suspend fun acceptReservation(tripId: String, reservationId: String): ReservationDto =
        errorMapper.execute { api.acceptReservation(tripId, reservationId) }

    override suspend fun rejectReservation(tripId: String, reservationId: String): ReservationDto =
        errorMapper.execute { api.rejectReservation(tripId, reservationId) }

    override suspend fun cancelReservation(tripId: String, reservationId: String): ReservationDto =
        errorMapper.execute { api.cancelReservation(tripId, reservationId) }
}

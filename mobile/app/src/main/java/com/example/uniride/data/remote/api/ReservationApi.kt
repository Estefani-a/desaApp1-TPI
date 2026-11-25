package com.example.uniride.data.remote.api

import com.example.uniride.data.remote.dto.ReservationDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Retrofit interface for RF04 reservation endpoints (docs/API.md §6). */
interface ReservationApi {

    @PUT("trips/{tripId}/reservations/{reservationId}")
    suspend fun requestSeat(
        @Path("tripId") tripId: String,
        @Path("reservationId") reservationId: String,
    ): ReservationDto

    @GET("trips/{tripId}/reservations")
    suspend fun getTripReservations(@Path("tripId") tripId: String): List<ReservationDto>

    @POST("trips/{tripId}/reservations/{reservationId}/accept")
    suspend fun acceptReservation(
        @Path("tripId") tripId: String,
        @Path("reservationId") reservationId: String,
    ): ReservationDto

    @POST("trips/{tripId}/reservations/{reservationId}/reject")
    suspend fun rejectReservation(
        @Path("tripId") tripId: String,
        @Path("reservationId") reservationId: String,
    ): ReservationDto

    @POST("trips/{tripId}/reservations/{reservationId}/cancel")
    suspend fun cancelReservation(
        @Path("tripId") tripId: String,
        @Path("reservationId") reservationId: String,
    ): ReservationDto
}

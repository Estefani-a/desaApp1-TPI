package com.example.uniride.data.remote.api

import com.example.uniride.data.remote.dto.CampusDto
import com.example.uniride.data.remote.dto.PublishTripRequest
import com.example.uniride.data.remote.dto.TripDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit interface for RF02 & RF03 trip and campus endpoints (docs/API.md §5). */
interface TripApi {

    @GET("trips")
    suspend fun getTrips(
        @Query("campus") campus: String? = null,
        @Query("direction") direction: String? = null,
        @Query("date") date: String? = null,
        @Query("shift") shift: String? = null,
        @Query("updatedSince") updatedSince: String? = null,
    ): List<TripDto>

    @GET("campuses")
    suspend fun getCampuses(): List<CampusDto>

    @GET("trips/{tripId}")
    suspend fun getTripDetail(@Path("tripId") tripId: String): TripDto

    @PUT("trips/{tripId}")
    suspend fun publishTrip(
        @Path("tripId") tripId: String,
        @Body request: PublishTripRequest,
    ): TripDto

    @GET("users/me/trips")
    suspend fun getMyTrips(): List<TripDto>

    @POST("trips/{tripId}/finish")
    suspend fun finishTrip(@Path("tripId") tripId: String): TripDto

    @POST("trips/{tripId}/cancel")
    suspend fun cancelTrip(@Path("tripId") tripId: String): TripDto
}

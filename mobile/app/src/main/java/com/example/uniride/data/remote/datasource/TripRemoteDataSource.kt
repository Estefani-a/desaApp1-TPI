package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.dto.CampusDto
import com.example.uniride.data.remote.dto.PublishTripRequest
import com.example.uniride.data.remote.dto.TripDto

interface TripRemoteDataSource {
    suspend fun getTrips(
        campus: String? = null,
        direction: String? = null,
        date: String? = null,
        shift: String? = null,
        updatedSince: String? = null,
    ): List<TripDto>

    suspend fun getCampuses(): List<CampusDto>
    suspend fun getTripDetail(tripId: String): TripDto
    suspend fun publishTrip(tripId: String, request: PublishTripRequest): TripDto
    suspend fun getMyTrips(): List<TripDto>
    suspend fun finishTrip(tripId: String): TripDto
    suspend fun cancelTrip(tripId: String): TripDto
}

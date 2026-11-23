package com.example.uniride.data.remote.datasource

import com.example.uniride.data.remote.api.TripApi
import com.example.uniride.data.remote.dto.CampusDto
import com.example.uniride.data.remote.dto.PublishTripRequest
import com.example.uniride.data.remote.dto.TripDto
import javax.inject.Inject

class RetrofitTripRemoteDataSource @Inject constructor(
    private val api: TripApi,
    private val errorMapper: RetrofitErrorMapper,
) : TripRemoteDataSource {

    override suspend fun getTrips(
        campus: String?,
        direction: String?,
        date: String?,
        shift: String?,
        updatedSince: String?,
    ): List<TripDto> = errorMapper.execute {
        api.getTrips(
            campus = campus,
            direction = direction,
            date = date,
            shift = shift,
            updatedSince = updatedSince,
        )
    }

    override suspend fun getCampuses(): List<CampusDto> =
        errorMapper.execute { api.getCampuses() }

    override suspend fun getTripDetail(tripId: String): TripDto =
        errorMapper.execute { api.getTripDetail(tripId) }

    override suspend fun publishTrip(tripId: String, request: PublishTripRequest): TripDto =
        errorMapper.execute { api.publishTrip(tripId, request) }

    override suspend fun getMyTrips(): List<TripDto> =
        errorMapper.execute { api.getMyTrips() }

    override suspend fun finishTrip(tripId: String): TripDto =
        errorMapper.execute { api.finishTrip(tripId) }

    override suspend fun cancelTrip(tripId: String): TripDto =
        errorMapper.execute { api.cancelTrip(tripId) }
}

package com.example.uniride.domain.usecase.trip

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripRoute
import com.example.uniride.domain.model.TripSchedule
import com.example.uniride.domain.validation.TripDraftError
import com.example.uniride.fake.FakeCampusRepository
import com.example.uniride.fake.FakeTripRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class PublishTripUseCaseTest {

    private val campus = Campus(CampusCode("MONSERRAT"), "Sede Monserrat", listOf("Salida Lima 717"))

    // 21:00 UTC is 18:00 in Buenos Aires on 2026-11-20
    private val clock = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
    private val trips = FakeTripRepository()
    private val campuses = FakeCampusRepository().apply { campuses.value = listOf(campus) }
    private val publish = PublishTripUseCase(trips, campuses, clock)

    private fun draft(seats: Int = 3, meetingPoint: String = "Salida Lima 717") = TripDraft(
        campus = campus.code,
        route = TripRoute(Direction.VUELTA, meetingPoint, "Ramos Mejía", null),
        schedule = TripSchedule(LocalDate.of(2026, 11, 20), LocalTime.of(22, 15), LocalTime.of(23, 0)),
        totalSeats = seats,
        contributionPerSeat = 3500,
    )

    @Test
    fun `un borrador valido se publica y devuelve el id`() = runTest {
        trips.publishResult = AppResult.Success(TripId("nuevo"))

        val result = publish(draft())

        assertEquals(PublishTripResult.Published(TripId("nuevo")), result)
        assertEquals(listOf(draft()), trips.publishedDrafts)
    }

    @Test
    fun `un borrador con errores no llega al repositorio`() = runTest {
        val result = publish(draft(seats = 5, meetingPoint = "Otra salida"))

        assertEquals(
            PublishTripResult.Invalid(setOf(TripDraftError.INVALID_SEATS, TripDraftError.MEETING_POINT_NOT_IN_CAMPUS)),
            result,
        )
        assertTrue(trips.publishedDrafts.isEmpty())
    }

    @Test
    fun `si la sede no esta guardada devuelve error de sede y no publica`() = runTest {
        campuses.campuses.value = emptyList()

        assertEquals(PublishTripResult.Invalid(setOf(TripDraftError.UNKNOWN_CAMPUS)), publish(draft()))
        assertTrue(trips.publishedDrafts.isEmpty())
    }

    @Test
    fun `el error del repositorio llega como Failed`() = runTest {
        trips.publishResult = AppResult.Failure(DataError.UNAUTHORIZED)
        assertEquals(PublishTripResult.Failed(DataError.UNAUTHORIZED), publish(draft()))

        trips.publishResult = AppResult.Failure(DataError.VALIDATION)
        assertEquals(PublishTripResult.Failed(DataError.VALIDATION), publish(draft()))
    }
}

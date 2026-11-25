package com.example.uniride.domain.usecase.trip

import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.DomainFixtures.trip
import com.example.uniride.domain.model.RejectedTrip
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.fake.FakeTripRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Use cases of C1 Inicio del conductor (RF02). "Now" is 2026-11-20 18:00. */
class DriverTripsUseCasesTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
    private val repository = FakeTripRepository()

    private fun saved(id: String, status: TripStatus = TripStatus.ABIERTO, date: LocalDate = LocalDate.of(2026, 11, 20), time: LocalTime = LocalTime.of(22, 15)) =
        trip(status = status, date = date, time = time).copy(id = TripId(id))

    @Test
    fun `los proximos son abiertos o completos que no salieron, el mas cercano primero`() = runTest {
        repository.publishedTrips.value = listOf(
            saved("manana", date = LocalDate.of(2026, 11, 21)),
            saved("completo", TripStatus.COMPLETO, time = LocalTime.of(20, 0)),
            saved("finalizado", TripStatus.FINALIZADO),
            saved("cancelado", TripStatus.CANCELADO),
            saved("pasado", time = LocalTime.of(9, 0)),
        )

        val upcoming = ObserveUpcomingDriverTripsUseCase(repository, clock)().first()

        assertEquals(listOf("completo", "manana"), upcoming.map { it.id.value })
    }

    @Test
    fun `un rechazo trae el viaje guardado y su motivo, o null si ya no esta`() = runTest {
        val rejectedTrip = saved("rechazado", TripStatus.CANCELADO)
        repository.publishedTrips.value = listOf(rejectedTrip)
        repository.rejectedTrips.value = mapOf(
            TripId("rechazado") to DataError.VALIDATION,
            TripId("borrado") to DataError.CONFLICT,
        )

        val rejected = ObserveRejectedTripsUseCase(repository)().first()

        assertEquals(
            listOf(
                RejectedTrip(TripId("rechazado"), rejectedTrip, DataError.VALIDATION),
                RejectedTrip(TripId("borrado"), null, DataError.CONFLICT),
            ),
            rejected,
        )
    }

    @Test
    fun `descartar un rechazo y refrescar delegan en el repositorio`() = runTest {
        repository.rejectedTrips.value = mapOf(TripId("rechazado") to DataError.VALIDATION)

        DismissTripRejectionUseCase(repository)(TripId("rechazado"))
        RefreshMyTripsUseCase(repository)()

        assertEquals(listOf(TripId("rechazado")), repository.dismissedRejections)
        assertEquals(1, repository.refreshMyTripsCalls)
    }
}

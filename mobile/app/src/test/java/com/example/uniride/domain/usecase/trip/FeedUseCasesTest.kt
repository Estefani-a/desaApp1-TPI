package com.example.uniride.domain.usecase.trip

import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.DomainFixtures.trip
import com.example.uniride.domain.model.Shift
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.fake.FakeFeedFiltersRepository
import com.example.uniride.fake.FakeTripRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** RF03: what the feed lists and how the filters are restored. "Now" is 2026-11-20 18:00. */
class FeedUseCasesTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
    private val today = LocalDate.of(2026, 11, 20)

    @Test
    fun `el feed lista abiertos y completos que no salieron, ordenados por salida`() = runTest {
        val late = trip(time = LocalTime.of(22, 15)).copy(id = TripId("late"))
        val tomorrow = trip(date = today.plusDays(1), time = LocalTime.of(8, 0)).copy(id = TripId("tomorrow"))
        val early = trip(time = LocalTime.of(19, 0), status = TripStatus.COMPLETO).copy(id = TripId("early"))
        val departed = trip(time = LocalTime.of(17, 59)).copy(id = TripId("departed"))
        val cancelled = trip(status = TripStatus.CANCELADO).copy(id = TripId("cancelled"))
        val repository = FakeTripRepository(listOf(tomorrow, late, departed, cancelled, early))

        val listed = ObserveTripsUseCase(repository, clock)(TripFilters()).first()

        assertEquals(listOf("early", "late", "tomorrow"), listed.map { it.id.value })
    }

    @Test
    fun `los filtros de sede, sentido, fecha y turno se combinan con Y`() = runTest {
        val match = trip(campus = CampusCode("MONSERRAT"), direction = Direction.VUELTA, time = LocalTime.of(22, 0))
            .copy(id = TripId("match"))
        val otherCampus = trip(campus = CampusCode("BELGRANO"), time = LocalTime.of(22, 0)).copy(id = TripId("campus"))
        val otherShift = trip(date = today.plusDays(1), time = LocalTime.of(9, 0)).copy(id = TripId("shift"))
        val otherDirection = trip(direction = Direction.IDA, time = LocalTime.of(22, 0)).copy(id = TripId("direction"))
        val repository = FakeTripRepository(listOf(match, otherCampus, otherShift, otherDirection))
        val filters = TripFilters(CampusCode("MONSERRAT"), Direction.VUELTA, today, Shift.NOCHE)

        val listed = ObserveTripsUseCase(repository, clock)(filters).first()

        assertEquals(listOf("match"), listed.map { it.id.value })
    }

    @Test
    fun `un viaje a la una de la manana entra en el turno noche`() = runTest {
        val lateNight = trip(date = today.plusDays(1), time = LocalTime.of(1, 0))
        val repository = FakeTripRepository(listOf(lateNight))

        val night = ObserveTripsUseCase(repository, clock)(TripFilters(shift = Shift.NOCHE)).first()
        val morning = ObserveTripsUseCase(repository, clock)(TripFilters(shift = Shift.MANANA)).first()

        assertEquals(1, night.size)
        assertEquals(0, morning.size)
    }

    @Test
    fun `los ultimos filtros se restauran al abrir`() = runTest {
        val saved = TripFilters(campus = CampusCode("RECOLETA"), shift = Shift.TARDE, date = today.plusDays(2))
        val filters = ObserveFeedFiltersUseCase(FakeFeedFiltersRepository(saved), clock)().first()

        assertEquals(saved, filters)
    }

    @Test
    fun `una fecha guardada que ya paso se descarta`() = runTest {
        val saved = TripFilters(campus = CampusCode("RECOLETA"), date = today.minusDays(1))

        val filters = ObserveFeedFiltersUseCase(FakeFeedFiltersRepository(saved), clock)().first()

        assertNull(filters.date)
        assertEquals(CampusCode("RECOLETA"), filters.campus)
    }

    @Test
    fun `guardar filtros los deja para la proxima vez`() = runTest {
        val repository = FakeFeedFiltersRepository()
        val filters = TripFilters(direction = Direction.IDA)

        SaveFeedFiltersUseCase(repository)(filters)

        assertEquals(filters, repository.saved.value)
    }
}

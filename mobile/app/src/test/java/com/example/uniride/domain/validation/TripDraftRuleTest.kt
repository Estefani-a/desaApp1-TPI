package com.example.uniride.domain.validation

import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.DomainFixtures.NOW
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripRoute
import com.example.uniride.domain.model.TripSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TripDraftRuleTest {

    private val monserrat = Campus(CampusCode("MONSERRAT"), "Sede Monserrat", listOf("Salida Lima 717", "Salida Chile 1142"))
    private val belgrano = Campus(CampusCode("BELGRANO"), "Sede Belgrano", listOf("Salida Echeverría"))

    private fun draft(
        seats: Int = 3,
        contribution: Int = 3500,
        meetingPoint: String = "Salida Lima 717",
        neighborhood: String = "Ramos Mejía",
        date: LocalDate = NOW.toLocalDate(),
        time: LocalTime = LocalTime.of(22, 15),
        arrival: LocalTime? = LocalTime.of(23, 0),
    ) = TripDraft(
        campus = monserrat.code,
        route = TripRoute(Direction.VUELTA, meetingPoint, neighborhood, null),
        schedule = TripSchedule(date, time, arrival),
        totalSeats = seats,
        contributionPerSeat = contribution,
    )

    private fun errors(draft: TripDraft = draft(), campus: Campus? = monserrat) =
        TripDraftRule.evaluate(draft, campus, NOW)

    @Test
    fun `un borrador completo y correcto no tiene errores`() {
        assertEquals(emptySet<TripDraftError>(), errors())
    }

    @Test
    fun `las plazas van de 1 a 4`() {
        assertEquals(setOf(TripDraftError.INVALID_SEATS), errors(draft(seats = 0)))
        assertEquals(emptySet<TripDraftError>(), errors(draft(seats = 1)))
        assertEquals(emptySet<TripDraftError>(), errors(draft(seats = 4)))
        assertEquals(setOf(TripDraftError.INVALID_SEATS), errors(draft(seats = 5)))
    }

    @Test
    fun `la salida tiene que ser posterior a ahora`() {
        val now = NOW.toLocalTime()
        assertEquals(
            setOf(TripDraftError.DEPARTURE_NOT_IN_FUTURE),
            errors(draft(time = now.minusMinutes(1), arrival = null)),
        )
        assertEquals(setOf(TripDraftError.DEPARTURE_NOT_IN_FUTURE), errors(draft(time = now, arrival = null)))
        assertEquals(emptySet<TripDraftError>(), errors(draft(time = now.plusMinutes(1), arrival = null)))
    }

    @Test
    fun `una fecha pasada es un error aunque la hora sea posterior`() {
        assertEquals(
            setOf(TripDraftError.DEPARTURE_NOT_IN_FUTURE),
            errors(draft(date = NOW.toLocalDate().minusDays(1))),
        )
    }

    @Test
    fun `el aporte no puede ser negativo pero puede ser cero`() {
        assertEquals(setOf(TripDraftError.NEGATIVE_CONTRIBUTION), errors(draft(contribution = -1)))
        assertEquals(emptySet<TripDraftError>(), errors(draft(contribution = 0)))
    }

    @Test
    fun `el punto de encuentro tiene que ser de la sede elegida`() {
        assertEquals(setOf(TripDraftError.MEETING_POINT_NOT_IN_CAMPUS), errors(draft(meetingPoint = "Salida Echeverría")))
        assertEquals(emptySet<TripDraftError>(), errors(draft(meetingPoint = "Salida Chile 1142")))
    }

    @Test
    fun `con otra sede cargada el punto de encuentro se valida contra esa sede`() {
        assertEquals(emptySet<TripDraftError>(), errors(draft(meetingPoint = "Salida Echeverría"), campus = belgrano))
    }

    @Test
    fun `sin sede el borrador tiene error de sede`() {
        assertEquals(setOf(TripDraftError.UNKNOWN_CAMPUS), errors(campus = null))
    }

    @Test
    fun `un punto de encuentro o un barrio en blanco son errores`() {
        assertEquals(setOf(TripDraftError.MEETING_POINT_EMPTY), errors(draft(meetingPoint = "  ")))
        assertEquals(setOf(TripDraftError.NEIGHBORHOOD_EMPTY), errors(draft(neighborhood = "")))
    }

    @Test
    fun `la llegada tiene que ser posterior a la salida`() {
        val departure = LocalTime.of(22, 15)
        assertEquals(
            setOf(TripDraftError.ARRIVAL_NOT_AFTER_DEPARTURE),
            errors(draft(time = departure, arrival = departure.minusMinutes(1))),
        )
        assertEquals(
            setOf(TripDraftError.ARRIVAL_NOT_AFTER_DEPARTURE),
            errors(draft(time = departure, arrival = departure)),
        )
        assertEquals(emptySet<TripDraftError>(), errors(draft(time = departure, arrival = departure.plusMinutes(1))))
    }

    @Test
    fun `sin hora de llegada no hay error de llegada`() {
        assertEquals(emptySet<TripDraftError>(), errors(draft(arrival = null)))
    }

    @Test
    fun `un borrador con varios problemas los devuelve todos`() {
        val result = errors(
            draft(
                seats = 5,
                contribution = -1,
                meetingPoint = "Salida Echeverría",
                neighborhood = " ",
                date = NOW.toLocalDate().minusDays(1),
                time = LocalTime.of(22, 15),
                arrival = LocalTime.of(21, 0),
            ),
        )

        assertEquals(
            setOf(
                TripDraftError.INVALID_SEATS,
                TripDraftError.NEGATIVE_CONTRIBUTION,
                TripDraftError.MEETING_POINT_NOT_IN_CAMPUS,
                TripDraftError.NEIGHBORHOOD_EMPTY,
                TripDraftError.DEPARTURE_NOT_IN_FUTURE,
                TripDraftError.ARRIVAL_NOT_AFTER_DEPARTURE,
            ),
            result,
        )
    }
}

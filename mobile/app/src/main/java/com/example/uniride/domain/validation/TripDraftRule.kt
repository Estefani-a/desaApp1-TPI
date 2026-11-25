package com.example.uniride.domain.validation

import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.TripDraft
import java.time.LocalDateTime

/** A field of the publish form (C2) that is not valid. The screen marks each one. */
enum class TripDraftError {
    /** The campus does not exist (`null` was given). */
    UNKNOWN_CAMPUS,

    /** The meeting point is blank. */
    MEETING_POINT_EMPTY,

    /** The meeting point is not one of the chosen campus. */
    MEETING_POINT_NOT_IN_CAMPUS,

    /** The destination neighborhood is blank. */
    NEIGHBORHOOD_EMPTY,

    /** The departure is not after now. */
    DEPARTURE_NOT_IN_FUTURE,

    /** The estimated arrival is not after the departure time. */
    ARRIVAL_NOT_AFTER_DEPARTURE,

    /** Seats are not between 1 and 4. */
    INVALID_SEATS,

    /** The contribution per seat is negative. */
    NEGATIVE_CONTRIBUTION,
}

/**
 * Whether a [TripDraft] can be published (RF02). Same rules as `PUT /trips/{tripId}`
 * (docs/API.md §5.4), so the form catches the errors before the server does. The vehicle is
 * not checked here: `TripRepository.publish` does it.
 *
 * Two rules are not in the API and are only for the form: the meeting point and the
 * neighborhood are not blank, and the arrival is after the departure.
 */
object TripDraftRule {

    const val MIN_SEATS = 1
    const val MAX_SEATS = 4

    /**
     * @param campus the campus chosen in [draft], or `null` if it was not found
     * @param now current time in Buenos Aires
     * @return the fields with errors; empty if the draft is valid
     */
    fun evaluate(draft: TripDraft, campus: Campus?, now: LocalDateTime): Set<TripDraftError> = buildSet {
        if (campus == null) add(TripDraftError.UNKNOWN_CAMPUS)

        val meetingPoint = draft.route.meetingPoint
        when {
            meetingPoint.isBlank() -> add(TripDraftError.MEETING_POINT_EMPTY)
            campus != null && meetingPoint !in campus.meetingPoints -> add(TripDraftError.MEETING_POINT_NOT_IN_CAMPUS)
        }

        if (draft.route.destinationNeighborhood.isBlank()) add(TripDraftError.NEIGHBORHOOD_EMPTY)

        val departure = LocalDateTime.of(draft.schedule.departureDate, draft.schedule.departureTime)
        if (!departure.isAfter(now)) add(TripDraftError.DEPARTURE_NOT_IN_FUTURE)

        val arrival = draft.schedule.estimatedArrivalTime
        if (arrival != null && !arrival.isAfter(draft.schedule.departureTime)) {
            add(TripDraftError.ARRIVAL_NOT_AFTER_DEPARTURE)
        }

        if (draft.totalSeats !in MIN_SEATS..MAX_SEATS) add(TripDraftError.INVALID_SEATS)
        if (draft.contributionPerSeat < 0) add(TripDraftError.NEGATIVE_CONTRIBUTION)
    }
}

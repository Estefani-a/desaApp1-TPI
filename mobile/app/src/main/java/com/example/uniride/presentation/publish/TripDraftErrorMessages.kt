package com.example.uniride.presentation.publish

import androidx.annotation.StringRes
import com.example.uniride.R
import com.example.uniride.domain.validation.TripDraftError

/** Field of the form that [this] error marks. */
val TripDraftError.field: PublishField
    get() = when (this) {
        TripDraftError.UNKNOWN_CAMPUS -> PublishField.CAMPUS
        TripDraftError.MEETING_POINT_EMPTY,
        TripDraftError.MEETING_POINT_NOT_IN_CAMPUS,
        -> PublishField.MEETING_POINT
        TripDraftError.NEIGHBORHOOD_EMPTY -> PublishField.NEIGHBORHOOD
        TripDraftError.DEPARTURE_NOT_IN_FUTURE -> PublishField.DEPARTURE
        TripDraftError.INVALID_SEATS -> PublishField.SEATS
        TripDraftError.NEGATIVE_CONTRIBUTION -> PublishField.CONTRIBUTION
    }

/** Text under the field for [this] error. */
@get:StringRes
val TripDraftError.messageRes: Int
    get() = when (this) {
        TripDraftError.UNKNOWN_CAMPUS -> R.string.publish_error_campus
        TripDraftError.MEETING_POINT_EMPTY,
        TripDraftError.MEETING_POINT_NOT_IN_CAMPUS,
        -> R.string.publish_error_meeting_point
        TripDraftError.NEIGHBORHOOD_EMPTY -> R.string.publish_error_neighborhood
        TripDraftError.DEPARTURE_NOT_IN_FUTURE -> R.string.publish_error_departure_past
        TripDraftError.INVALID_SEATS -> R.string.publish_error_seats
        TripDraftError.NEGATIVE_CONTRIBUTION -> R.string.publish_error_contribution
    }

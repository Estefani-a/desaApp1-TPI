package com.example.uniride.domain.model

import com.example.uniride.core.result.DataError

/**
 * A trip the server refused when it was synced. [trip] is what was saved on the phone, or
 * `null` if it is not there anymore; [error] is the reason.
 */
data class RejectedTrip(
    val tripId: TripId,
    val trip: Trip?,
    val error: DataError,
)

package com.example.uniride.domain.usecase.reservation

import com.example.uniride.core.result.DataError

/** What happened when the driver answered a request or the passenger cancelled. */
sealed interface ReservationChangeResult {
    /** Saved on the phone; it is sent to the server when there is connection. */
    data object Done : ReservationChangeResult

    /** The rules do not allow it; nothing was saved. */
    data class NotAllowed(val reason: ReservationChangeBlock) : ReservationChangeResult

    data class Failed(val error: DataError) : ReservationChangeResult
}

/** Why a change of a reservation is not allowed. The screen shows a message for each one. */
enum class ReservationChangeBlock {
    /** The trip already started: the reservations no longer change (docs/API.md §6.3). */
    TRIP_ALREADY_DEPARTED,

    /** The trip has no seats left to accept another request. */
    NO_SEATS,

    /** Only a pending request can be accepted or rejected. */
    NOT_PENDING,

    /** Only a pending or accepted reservation can be cancelled. */
    NOT_ACTIVE,
}

package com.example.uniride.presentation.reservations

import androidx.annotation.StringRes
import com.example.uniride.R
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.usecase.reservation.ReservationChangeBlock
import com.example.uniride.domain.validation.SeatRequestDecision

/*
 * Texts of the RF04 reasons. The domain says why; the screens say it in Spanish (strings.xml).
 * Who sees the message matters: "no se puede cancelar" is for the passenger, "no se pueden
 * responder" for the driver.
 */

/** Why "Solicitar asiento" is not offered. `null` when it is allowed. */
@get:StringRes
val SeatRequestDecision.messageRes: Int?
    get() = when (this) {
        SeatRequestDecision.ALLOWED -> null
        SeatRequestDecision.OWN_TRIP -> R.string.seat_decision_own_trip
        SeatRequestDecision.ALREADY_RESERVED -> R.string.seat_decision_already_reserved
        SeatRequestDecision.TRIP_NOT_OPEN -> R.string.seat_decision_trip_not_open
        SeatRequestDecision.NO_SEATS -> R.string.seat_decision_no_seats
        SeatRequestDecision.DEPARTED -> R.string.seat_decision_departed
    }

/** Why the passenger could not cancel, said to the passenger. */
@StringRes
fun ReservationChangeBlock.passengerMessageRes(): Int = when (this) {
    ReservationChangeBlock.TRIP_ALREADY_DEPARTED -> R.string.change_block_cancel_departed
    else -> R.string.change_block_not_active
}

/** Why the driver could not answer a request, said to the driver. */
@StringRes
fun ReservationChangeBlock.driverMessageRes(): Int = when (this) {
    ReservationChangeBlock.TRIP_ALREADY_DEPARTED -> R.string.change_block_respond_departed
    ReservationChangeBlock.NO_SEATS -> R.string.change_block_no_seats
    else -> R.string.change_block_not_pending
}

/** Why the server did not accept a change of the passenger (request or cancel). */
@StringRes
fun ReservationRejection.passengerMessageRes(): Int = when (this) {
    ReservationRejection.NO_SEATS -> R.string.rejection_no_seats
    ReservationRejection.TRIP_NOT_OPEN -> R.string.rejection_trip_not_open
    ReservationRejection.ALREADY_RESERVED -> R.string.rejection_already_reserved
    ReservationRejection.TRIP_ALREADY_DEPARTED -> R.string.rejection_cancel_departed
    ReservationRejection.OTHER -> R.string.rejection_other
}

/** Why the server did not accept an answer of the driver. */
@StringRes
fun ReservationRejection.driverMessageRes(): Int = when (this) {
    ReservationRejection.NO_SEATS -> R.string.rejection_respond_no_seats
    ReservationRejection.TRIP_ALREADY_DEPARTED -> R.string.rejection_respond_departed
    ReservationRejection.TRIP_NOT_OPEN -> R.string.rejection_trip_not_open
    else -> R.string.rejection_other
}

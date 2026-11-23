package com.example.uniride.presentation.common

import androidx.annotation.StringRes
import com.example.uniride.R
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.Shift
import com.example.uniride.domain.model.TripStatus

/*
 * How the values of the glossary (CONVENCIONES §5) are shown in the UI. The enums keep the names
 * of the preentrega; the texts live in strings.xml.
 */

@get:StringRes
val Direction.labelRes: Int
    get() = when (this) {
        Direction.IDA -> R.string.direction_ida
        Direction.VUELTA -> R.string.direction_vuelta
    }

@get:StringRes
val Shift.labelRes: Int
    get() = when (this) {
        Shift.MANANA -> R.string.shift_manana
        Shift.TARDE -> R.string.shift_tarde
        Shift.NOCHE -> R.string.shift_noche
    }

@get:StringRes
val TripStatus.labelRes: Int
    get() = when (this) {
        TripStatus.ABIERTO -> R.string.trip_status_abierto
        TripStatus.COMPLETO -> R.string.trip_status_completo
        TripStatus.FINALIZADO -> R.string.trip_status_finalizado
        TripStatus.CANCELADO -> R.string.trip_status_cancelado
    }

@get:StringRes
val ReservationStatus.labelRes: Int
    get() = when (this) {
        ReservationStatus.PENDIENTE -> R.string.reservation_status_pendiente
        ReservationStatus.ACEPTADA -> R.string.reservation_status_aceptada
        ReservationStatus.RECHAZADA -> R.string.reservation_status_rechazada
        ReservationStatus.CANCELADA -> R.string.reservation_status_cancelada
    }

val TripStatus.tone: StatusTone
    get() = when (this) {
        TripStatus.ABIERTO -> StatusTone.POSITIVE
        TripStatus.COMPLETO -> StatusTone.WARNING
        TripStatus.FINALIZADO -> StatusTone.NEUTRAL
        TripStatus.CANCELADO -> StatusTone.NEGATIVE
    }

val ReservationStatus.tone: StatusTone
    get() = when (this) {
        ReservationStatus.PENDIENTE -> StatusTone.WARNING
        ReservationStatus.ACEPTADA -> StatusTone.POSITIVE
        ReservationStatus.RECHAZADA -> StatusTone.NEGATIVE
        ReservationStatus.CANCELADA -> StatusTone.NEUTRAL
    }

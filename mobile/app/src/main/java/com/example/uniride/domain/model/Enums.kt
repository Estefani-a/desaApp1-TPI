package com.example.uniride.domain.model

import java.time.LocalTime

/*
 * Enums of the glossary (CONVENCIONES §5). The names of the constants are the values of the
 * model of the preentrega, the same in the app, the API and the database.
 */

/** Usual mode chosen at registration. */
enum class PreferredMode { PASAJERO, CONDUCTOR, AMBOS }

/** `IDA`: to the campus. `VUELTA`: from the campus. */
enum class Direction { IDA, VUELTA }

enum class TripStatus { ABIERTO, COMPLETO, FINALIZADO, CANCELADO }

enum class ReservationStatus {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    CANCELADA,
    ;

    /** Holds or may hold a seat. A passenger has at most one active reservation per trip. */
    val isActive: Boolean get() = this == PENDIENTE || this == ACEPTADA
}

/**
 * Mode the app is showing right now. Unlike [PreferredMode] it has no `AMBOS`: a user with
 * `AMBOS` switches between the two.
 */
enum class AppMode { PASAJERO, CONDUCTOR }

/**
 * Time slot of the day (docs/API.md §3.1), derived from the departure time. Half-open intervals
 * `[start, end)`: MANANA 06:00 to 12:59, TARDE 13:00 to 17:59 and NOCHE 18:00 to 05:59, which goes
 * through midnight (a trip at 01:00 is a night trip). The server filters with the same rule.
 */
enum class Shift(val start: LocalTime, val end: LocalTime) {
    MANANA(LocalTime.of(6, 0), LocalTime.of(13, 0)),
    TARDE(LocalTime.of(13, 0), LocalTime.of(18, 0)),
    NOCHE(LocalTime.of(18, 0), LocalTime.of(6, 0)),
    ;

    operator fun contains(time: LocalTime): Boolean {
        val afterStart = !time.isBefore(start)
        val beforeEnd = time.isBefore(end)
        return if (end.isBefore(start)) afterStart || beforeEnd else afterStart && beforeEnd
    }

    companion object {
        fun of(time: LocalTime): Shift = entries.first { time in it }
    }
}

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

/** Time slot of the day. Each one goes from its start to the start of the next one. */
enum class Shift(val start: LocalTime) {
    /** 00:00 to 12:59. */
    MANANA(LocalTime.MIDNIGHT),

    /** 13:00 to 17:59. */
    TARDE(LocalTime.of(13, 0)),

    /** 18:00 to 23:59. */
    NOCHE(LocalTime.of(18, 0)),
    ;

    companion object {
        fun of(time: LocalTime): Shift = entries.last { !time.isBefore(it.start) }
    }
}

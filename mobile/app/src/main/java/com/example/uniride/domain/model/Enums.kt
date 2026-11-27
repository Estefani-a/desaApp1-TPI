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

/** Kind of push the server sends (docs/API.md §9). */
enum class NotificationType { NUEVA_SOLICITUD, SOLICITUD_ACEPTADA, SOLICITUD_RECHAZADA }

/**
 * Mode the app is showing right now. Unlike [PreferredMode] it has no `AMBOS`: a user with
 * `AMBOS` switches between the two.
 */
enum class AppMode { PASAJERO, CONDUCTOR }

/**
 * Time slot of the day, from the departure time. Half-open intervals, as in the API contract;
 * NOCHE crosses midnight.
 */
enum class Shift(val start: LocalTime) {
    /** [06:00, 13:00). */
    MANANA(LocalTime.of(6, 0)),

    /** [13:00, 18:00). */
    TARDE(LocalTime.of(13, 0)),

    /** [18:00, 06:00): before 06:00 it is still the night of the previous evening. */
    NOCHE(LocalTime.of(18, 0)),
    ;

    companion object {
        fun of(time: LocalTime): Shift = entries.lastOrNull { !time.isBefore(it.start) } ?: NOCHE
    }
}

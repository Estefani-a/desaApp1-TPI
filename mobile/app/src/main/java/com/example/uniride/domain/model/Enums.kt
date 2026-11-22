package com.example.uniride.domain.model

import java.time.LocalTime

/**
 * Enums of the glossary (CONVENCIONES §5) and of docs/API.md §3.1. The names of the
 * constants are the values the API sends and Room stores, so they are not translated.
 */

/** Usual mode chosen at registration. */
enum class PreferredMode { PASAJERO, CONDUCTOR, AMBOS }

/** `IDA`: to the campus. `VUELTA`: from the campus. */
enum class Direction { IDA, VUELTA }

enum class TripStatus { ABIERTO, COMPLETO, FINALIZADO, CANCELADO }

enum class ReservationStatus { PENDIENTE, ACEPTADA, RECHAZADA, CANCELADA }

/**
 * Mode the app is showing right now (selector of the bottom bar). Unlike [PreferredMode]
 * it has no `AMBOS`: a user with `AMBOS` switches between the two.
 */
enum class AppMode { PASAJERO, CONDUCTOR }

/** Time slot of the `shift` filter (docs/API.md §3.1 and §11.1). */
enum class Shift {
    /** 00:00 to 12:59. */
    MANANA,

    /** 13:00 to 17:59. */
    TARDE,

    /** 18:00 to 23:59. */
    NOCHE,
    ;

    companion object {
        fun of(time: LocalTime): Shift = when {
            time.hour < 13 -> MANANA
            time.hour < 18 -> TARDE
            else -> NOCHE
        }
    }
}

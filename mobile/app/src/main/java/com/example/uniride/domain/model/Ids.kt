package com.example.uniride.domain.model

/*
 * Typed identifiers: a TripId cannot be passed where a ReservationId is expected, and the
 * compiler catches swapped arguments. They are value classes, so at runtime they are plain
 * Strings.
 */

@JvmInline
value class UserId(val value: String)

@JvmInline
value class TripId(val value: String)

@JvmInline
value class ReservationId(val value: String)

/** Code of a UADE campus, for example `MONSERRAT`. The list of campuses comes from the server. */
@JvmInline
value class CampusCode(val value: String)

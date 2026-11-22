package com.example.uniride.backend.trip;

import java.time.LocalTime;

/**
 * Time slot used by the {@code shift} filter of {@code GET /trips} (docs/API.md §3.1). It is
 * not stored: it is derived from the departure time.
 */
public enum Shift {
    MANANA(LocalTime.MIDNIGHT, LocalTime.of(12, 59)),
    TARDE(LocalTime.of(13, 0), LocalTime.of(17, 59)),
    NOCHE(LocalTime.of(18, 0), LocalTime.of(23, 59));

    private final LocalTime from;
    private final LocalTime to;

    Shift(LocalTime from, LocalTime to) {
        this.from = from;
        this.to = to;
    }

    /** First minute of the slot. */
    public LocalTime from() {
        return from;
    }

    /** Last minute of the slot, inclusive ({@code 12:59} includes {@code 12:59:59}). */
    public LocalTime to() {
        return to;
    }

    public static Shift of(LocalTime time) {
        if (time.getHour() < 13) {
            return MANANA;
        }
        return time.getHour() < 18 ? TARDE : NOCHE;
    }
}

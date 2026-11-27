package com.example.uniride.backend.model;

import java.time.LocalTime;

/**
 * Time slot used by the {@code shift} filter of {@code GET /trips} (docs/API.md §3.1). It is
 * not stored: it is derived from the departure time. Half-open intervals; NOCHE crosses
 * midnight: MANANA [06:00, 13:00), TARDE [13:00, 18:00), NOCHE [18:00, 06:00).
 */
public enum Shift {
    MANANA(LocalTime.of(6, 0)),
    TARDE(LocalTime.of(13, 0)),
    NOCHE(LocalTime.of(18, 0));

    private final LocalTime start;

    Shift(LocalTime start) {
        this.start = start;
    }

    /** First minute of the slot. Each slot ends where the next one starts. */
    public LocalTime start() {
        return start;
    }

    /** First minute after the slot: the start of the next one, derived so slots never overlap. */
    public LocalTime end() {
        Shift[] shifts = values();
        return shifts[(ordinal() + 1) % shifts.length].start;
    }

    /** {@code true} for NOCHE: it starts one day and ends the next one. */
    public boolean wrapsMidnight() {
        return !end().isAfter(start);
    }

    public static Shift of(LocalTime time) {
        // Before the first start (00:00 to 05:59) it is still the night of the previous evening.
        Shift result = NOCHE;
        for (Shift shift : values()) {
            if (!time.isBefore(shift.start)) {
                result = shift;
            }
        }
        return result;
    }
}

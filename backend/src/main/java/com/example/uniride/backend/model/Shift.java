package com.example.uniride.backend.model;

import java.time.LocalTime;

/**
 * Time slot used by the {@code shift} filter of {@code GET /trips} (docs/API.md §3.1). It is
 * not stored: it is derived from the departure time. Each slot goes from its start to the
 * start of the next one: MANANA 00:00–12:59, TARDE 13:00–17:59, NOCHE 18:00–23:59.
 */
public enum Shift {
    MANANA(LocalTime.MIDNIGHT),
    TARDE(LocalTime.of(13, 0)),
    NOCHE(LocalTime.of(18, 0));

    private final LocalTime start;

    Shift(LocalTime start) {
        this.start = start;
    }

    public LocalTime start() {
        return start;
    }

    public static Shift of(LocalTime time) {
        Shift result = MANANA;
        for (Shift shift : values()) {
            if (!time.isBefore(shift.start)) {
                result = shift;
            }
        }
        return result;
    }
}

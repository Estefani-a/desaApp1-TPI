package com.example.uniride.backend.model;

import java.util.EnumSet;
import java.util.Set;

/** Trip status (docs/API.md §3.1). */
public enum TripStatus {
    ABIERTO,
    COMPLETO,
    FINALIZADO,
    CANCELADO;

    /** No more changes of reservations. */
    public static final Set<TripStatus> CLOSED = EnumSet.of(FINALIZADO, CANCELADO);

    /**
     * The ones that still take passengers or may free a seat: what {@code GET /trips} lists
     * without {@code updatedSince} (docs/API.md §5.1). Defined as the complement of {@link #CLOSED}.
     */
    public static final Set<TripStatus> LISTED = EnumSet.complementOf(EnumSet.copyOf(CLOSED));

    public boolean isClosed() {
        return CLOSED.contains(this);
    }
}

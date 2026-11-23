package com.example.uniride.backend.model;

import java.util.EnumSet;
import java.util.Set;

/** Reservation status (docs/API.md §3.1). */
public enum ReservationStatus {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    CANCELADA;

    /** Statuses that hold or may hold a seat. A passenger has at most one per trip. */
    public static final Set<ReservationStatus> ACTIVE = EnumSet.of(PENDIENTE, ACEPTADA);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}

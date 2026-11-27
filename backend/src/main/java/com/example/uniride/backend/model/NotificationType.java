package com.example.uniride.backend.model;

/**
 * Push notification types sent for RF04 reservation lifecycle events (docs/API.md §3.1, §9).
 * {@code CALIFICAR_VIAJE} is out of scope for this release.
 */
public enum NotificationType {
    NUEVA_SOLICITUD,
    SOLICITUD_ACEPTADA,
    SOLICITUD_RECHAZADA
}

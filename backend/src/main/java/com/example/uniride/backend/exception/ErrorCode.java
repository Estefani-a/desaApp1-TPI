package com.example.uniride.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Error codes of the API contract (docs/API.md §2). The {@code code} field of every error
 * response is the name of one of these constants.
 */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Hay campos con errores."),
    INVALID_EMAIL_DOMAIN(HttpStatus.BAD_REQUEST, "El correo tiene que ser @uade.edu.ar."),
    WEAK_PASSWORD(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres, con letras y números."),
    INVALID_OR_EXPIRED_TOKEN(HttpStatus.BAD_REQUEST, "El enlace es inválido o venció."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Tenés que iniciar sesión."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."),
    ACCOUNT_NOT_ACTIVATED(HttpStatus.UNAUTHORIZED, "Todavía no activaste la cuenta. Revisá tu correo."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "No tenés permiso para hacer esto."),
    CANNOT_RESERVE_OWN_TRIP(HttpStatus.FORBIDDEN, "No podés reservar un asiento en tu propio viaje."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "No encontramos lo que buscabas."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "La ruta no admite ese método HTTP."),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo."),
    ID_CONFLICT(HttpStatus.CONFLICT, "El identificador ya está en uso."),
    NO_SEATS_AVAILABLE(HttpStatus.CONFLICT, "El viaje ya no tiene plazas disponibles."),
    TRIP_NOT_OPEN(HttpStatus.CONFLICT, "El viaje ya no está disponible."),
    ALREADY_RESERVED(HttpStatus.CONFLICT, "Ya tenés una reserva en este viaje."),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "No se puede hacer ese cambio de estado."),
    TRIP_ALREADY_DEPARTED(HttpStatus.CONFLICT, "El viaje ya empezó: sus reservas no se pueden cambiar."),
    ALREADY_RATED(HttpStatus.CONFLICT, "Ya calificaste a esta persona en este viaje."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El cuerpo tiene que ser JSON."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Probá de nuevo más tarde.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}

package com.example.uniride.backend.model;

/** An account stays {@code PENDIENTE} until the user opens the activation link (RF01). */
public enum AccountStatus {
    PENDIENTE,
    ACTIVA
}

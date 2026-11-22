package com.example.uniride.backend.user;

/** An account stays {@code PENDIENTE} until the user opens the activation link (RF01). */
public enum AccountStatus {
    PENDIENTE,
    ACTIVA
}

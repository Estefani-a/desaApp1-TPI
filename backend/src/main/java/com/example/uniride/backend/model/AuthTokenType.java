package com.example.uniride.backend.model;

/** Single-use email verification tokens for RF01 (docs/API.md §4.2 and §4.5). */
public enum AuthTokenType {
    ACTIVATION,
    PASSWORD_RESET
}

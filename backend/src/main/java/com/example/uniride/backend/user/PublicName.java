package com.example.uniride.backend.user;

/**
 * How a user appears to the others: first name and initial of the last name, "Lucía F."
 * (RNF07, docs/API.md §3.2). Only {@code GET/PUT /users/me} shows the full name.
 */
public final class PublicName {

    private PublicName() {
    }

    public static String of(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length < 2) {
            return parts[0];
        }
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}

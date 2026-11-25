package com.example.uniride.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

/**
 * What the user can edit about themselves: the same data at registration and in
 * {@code PUT /users/me}. The email is not here because it cannot change.
 */
@Embeddable
public class UserProfile {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String career;

    @Column(nullable = false, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_mode", nullable = false, length = 20)
    private PreferredMode preferredMode;

    /** {@code null} if the user did not load a vehicle. */
    @Embedded
    private Vehicle vehicle;

    protected UserProfile() {
    }

    public UserProfile(String fullName, String career, String phone, PreferredMode preferredMode, Vehicle vehicle) {
        this.fullName = Objects.requireNonNull(fullName);
        this.career = Objects.requireNonNull(career);
        this.phone = Objects.requireNonNull(phone);
        this.preferredMode = Objects.requireNonNull(preferredMode);
        this.vehicle = vehicle;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCareer() {
        return career;
    }

    public String getPhone() {
        return phone;
    }

    public PreferredMode getPreferredMode() {
        return preferredMode;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}

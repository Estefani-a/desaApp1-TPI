package com.example.uniride.backend.dto;

import com.example.uniride.backend.model.PreferredMode;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.model.UserProfile;

import java.util.UUID;

/** Authenticated user's own profile (docs/API.md §3.2). */
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String career,
        String phone,
        PreferredMode preferredMode,
        Double ratingAvg,
        int ratingCount,
        VehicleDTO vehicle
) {
    public static UserResponse from(User user) {
        UserProfile profile = user.getProfile();
        return new UserResponse(
                user.getId(),
                profile.getFullName(),
                user.getEmail(),
                profile.getCareer(),
                profile.getPhone(),
                profile.getPreferredMode(),
                user.getRatingAvg(),
                user.getRatingCount(),
                VehicleDTO.from(profile.getVehicle())
        );
    }
}

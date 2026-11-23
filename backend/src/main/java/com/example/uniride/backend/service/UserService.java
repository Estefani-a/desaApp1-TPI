package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.UpdateProfileRequest;
import com.example.uniride.backend.dto.UserResponse;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.model.UserProfile;
import com.example.uniride.backend.model.Vehicle;
import com.example.uniride.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Business rules for RF01 profile and FCM device-token endpoints (docs/API.md §4.6).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final Clock clock;

    public UserService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(requireUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        Vehicle vehicle = request.vehicle() != null ? request.vehicle().toEntity() : null;
        UserProfile updatedProfile = new UserProfile(
                request.fullName().trim(),
                request.career().trim(),
                request.phone().trim(),
                request.preferredMode(),
                vehicle
        );
        user.updateProfile(updatedProfile, clock.instant());
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional
    public void updateDeviceToken(UUID userId, String fcmToken) {
        User user = requireUser(userId);
        user.updateFcmToken(fcmToken.trim(), clock.instant());
        userRepository.save(user);
    }

    @Transactional
    public void deleteDeviceToken(UUID userId) {
        User user = requireUser(userId);
        user.clearFcmToken(clock.instant());
        userRepository.save(user);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }
}

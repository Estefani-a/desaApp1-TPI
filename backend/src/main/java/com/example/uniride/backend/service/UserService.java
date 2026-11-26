package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.UpdateProfileRequestDTO;
import com.example.uniride.backend.dto.UserResponseDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
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
    public UserResponseDTO getProfile(UUID userId) {
        return UserResponseDTO.from(requireUser(userId));
    }

    @Transactional
    public UserResponseDTO updateProfile(UUID userId, UpdateProfileRequestDTO request) {
        User user = requireUser(userId);
        user.updateProfile(request.toProfile(), clock.instant());
        userRepository.save(user);
        return UserResponseDTO.from(user);
    }

    @Transactional
    public void updateDeviceToken(UUID userId, String fcmToken) {
        User user = requireUser(userId);
        String normalizedToken = fcmToken.trim();
        Instant now = clock.instant();
        // Detach the token from any previous account on the same device (e.g. after offline logout).
        userRepository.clearFcmTokenFromOtherUsers(normalizedToken, userId, now);
        user.updateFcmToken(normalizedToken, now);
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

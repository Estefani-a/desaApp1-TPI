package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.UpdateProfileRequest;
import com.example.uniride.backend.dto.UserResponse;
import com.example.uniride.backend.dto.VehicleDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.PreferredMode;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.model.UserProfile;
import com.example.uniride.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T15:00:00Z");

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void actualizarPerfilMantieneElCorreoYGuardaVehiculo() {
        User user = new User(
                "lfernandez@uade.edu.ar",
                "$2a$10$hash",
                new UserProfile("Lucía", "Sistemas", "+5491100001111", PreferredMode.PASAJERO, null),
                NOW.minusSeconds(60)
        );
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Lucía Fernández",
                "Ingeniería en Informática",
                "+5491155550000",
                PreferredMode.AMBOS,
                new VehicleDTO("Volkswagen", "Gol Trend", "Gris", "ab123cd")
        );

        UserResponse updated = userService.updateProfile(user.getId(), request);

        assertThat(updated.email()).isEqualTo("lfernandez@uade.edu.ar");
        assertThat(updated.fullName()).isEqualTo("Lucía Fernández");
        assertThat(updated.preferredMode()).isEqualTo(PreferredMode.AMBOS);
        assertThat(updated.vehicle().plate()).isEqualTo("AB123CD");
        verify(userRepository).save(user);
    }

    @Test
    void actualizarYBorrarTokenFcmDelDispositivo() {
        User user = new User(
                "lfernandez@uade.edu.ar",
                "$2a$10$hash",
                new UserProfile("Lucía", "Sistemas", "+5491100001111", PreferredMode.PASAJERO, null),
                NOW
        );
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.updateDeviceToken(user.getId(), "fcm-token-abc");
        assertThat(user.getFcmToken()).isEqualTo("fcm-token-abc");

        userService.deleteDeviceToken(user.getId());
        assertThat(user.getFcmToken()).isNull();
    }

    @Test
    void consultarPerfilInexistenteDevuelveNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(unknownId))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}

package com.example.uniride.backend.service;

import com.example.uniride.backend.TestData;
import com.example.uniride.backend.dto.UpdateProfileRequestDTO;
import com.example.uniride.backend.dto.UserResponseDTO;
import com.example.uniride.backend.dto.VehicleDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.PreferredMode;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.example.uniride.backend.TestData.CLOCK;
import static com.example.uniride.backend.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, CLOCK);
    }

    @Test
    void actualizarPerfilMantieneElCorreoYGuardaVehiculoNormalizado() {
        User user = TestData.passenger();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(
                "  Martín Gómez Actualizado  ",
                "  Ingeniería en Informática ",
                " +5491155550000 ",
                PreferredMode.AMBOS,
                new VehicleDTO(" Volkswagen ", " Gol Trend ", " Gris ", " ab123cd ")
        );

        UserResponseDTO updated = userService.updateProfile(user.getId(), request);

        assertThat(updated.email()).isEqualTo(user.getEmail());
        assertThat(updated.fullName()).isEqualTo("Martín Gómez Actualizado");
        assertThat(updated.preferredMode()).isEqualTo(PreferredMode.AMBOS);
        assertThat(updated.vehicle().plate()).isEqualTo("AB123CD");
        verify(userRepository).save(user);
    }

    @Test
    void actualizarTokenFcmDesvinculaAOtroUsuarioYGuardaElNuevo() {
        User user = TestData.driver();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.updateDeviceToken(user.getId(), "  fcm-token-abc  ");

        verify(userRepository).clearFcmTokenFromOtherUsers("fcm-token-abc", user.getId(), NOW);
        assertThat(user.getFcmToken()).isEqualTo("fcm-token-abc");
        verify(userRepository).save(user);
    }

    @Test
    void borrarTokenFcmLoDejaEnNull() {
        User user = TestData.driver();
        user.updateFcmToken("fcm-token-abc", NOW);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.deleteDeviceToken(user.getId());

        assertThat(user.getFcmToken()).isNull();
        verify(userRepository).save(user);
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

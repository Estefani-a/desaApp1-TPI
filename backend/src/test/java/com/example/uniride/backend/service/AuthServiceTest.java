package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.ActivateRequestDTO;
import com.example.uniride.backend.dto.AuthResponseDTO;
import com.example.uniride.backend.dto.ConfirmPasswordResetRequestDTO;
import com.example.uniride.backend.dto.LoginRequestDTO;
import com.example.uniride.backend.dto.MessageResponseDTO;
import com.example.uniride.backend.dto.PasswordResetRequestDTO;
import com.example.uniride.backend.dto.RegisterRequestDTO;
import com.example.uniride.backend.dto.ResendActivationRequestDTO;
import com.example.uniride.backend.dto.VehicleDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.AccountStatus;
import com.example.uniride.backend.model.AuthToken;
import com.example.uniride.backend.model.AuthTokenType;
import com.example.uniride.backend.model.PreferredMode;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.model.UserProfile;
import com.example.uniride.backend.repository.AuthTokenRepository;
import com.example.uniride.backend.repository.UserRepository;
import com.example.uniride.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T15:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthTokenRepository authTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthMailService authMailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                authTokenRepository,
                passwordEncoder,
                jwtService,
                authMailService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void registroConCorreoUadeCreaCuentaPendienteYEnviaCorreo() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "  Lucia.F@UADE.EDU.AR ",
                "ñandú123",
                "Lucía Fernández",
                "Ingeniería en Informática",
                "+5491155550000",
                PreferredMode.AMBOS,
                new VehicleDTO("Volkswagen", "Gol Trend", "Gris", "ab123cd")
        );
        when(userRepository.existsByEmailIgnoreCase("lucia.f@uade.edu.ar")).thenReturn(false);
        when(passwordEncoder.encode("ñandú123")).thenReturn("$2a$10$hash");

        MessageResponseDTO response = authService.register(request);

        assertThat(response.message()).contains("activar");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("lucia.f@uade.edu.ar");
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.PENDIENTE);
        assertThat(saved.getVehicle().getPlate()).isEqualTo("AB123CD");

        verify(authTokenRepository).save(any(AuthToken.class));
        verify(authMailService).sendActivationEmail(eq("lucia.f@uade.edu.ar"), eq("Lucía Fernández"), any());
    }

    @Test
    void registroConDominioExternoDevuelveInvalidEmailDomain() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "usuario@gmail.com",
                "Secreta123",
                "Juan Pérez",
                "Ingeniería",
                "+5491155550000",
                PreferredMode.PASAJERO,
                null
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_EMAIL_DOMAIN);
        verifyNoInteractions(userRepository, authMailService);
    }

    @Test
    void registroConContrasenaDebilDevuelveWeakPassword() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "usuario@uade.edu.ar",
                "sololetas",
                "Juan Pérez",
                "Ingeniería",
                "+5491155550000",
                PreferredMode.PASAJERO,
                null
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.WEAK_PASSWORD);
        verifyNoInteractions(userRepository, authMailService);
    }

    @Test
    void registroConCorreoDuplicadoDevuelveEmailAlreadyRegistered() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "lfernandez@uade.edu.ar",
                "Secreta123",
                "Lucía Fernández",
                "Ingeniería",
                "+5491155550000",
                PreferredMode.PASAJERO,
                null
        );
        when(userRepository.existsByEmailIgnoreCase("lfernandez@uade.edu.ar")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
        verify(authMailService, never()).sendActivationEmail(any(), any(), any());
    }

    @Test
    void activarCuentaConTokenValidoActivaYDevuelveJwt() {
        User user = createUser("lfernandez@uade.edu.ar", false);
        String rawToken = "token-valido-123";
        AuthToken token = new AuthToken(
                AuthService.hashToken(rawToken),
                user,
                AuthTokenType.ACTIVATION,
                NOW.plus(Duration.ofHours(24)),
                NOW
        );
        Instant expiresAt = NOW.plus(Duration.ofDays(30));
        when(authTokenRepository.findByTokenHashAndType(AuthService.hashToken(rawToken), AuthTokenType.ACTIVATION))
                .thenReturn(Optional.of(token));
        when(jwtService.issue(user.getId())).thenReturn(new JwtService.IssuedToken("jwt-token", expiresAt));

        AuthResponseDTO response = authService.activate(new ActivateRequestDTO(rawToken));

        assertThat(user.isActive()).isTrue();
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(response.user().email()).isEqualTo("lfernandez@uade.edu.ar");
    }

    @Test
    void activarCuentaConTokenVencidoDevuelveInvalidOrExpiredToken() {
        User user = createUser("lfernandez@uade.edu.ar", false);
        String rawToken = "token-vencido";
        AuthToken expired = new AuthToken(
                AuthService.hashToken(rawToken),
                user,
                AuthTokenType.ACTIVATION,
                NOW.minusSeconds(1),
                NOW.minus(Duration.ofHours(25))
        );
        when(authTokenRepository.findByTokenHashAndType(AuthService.hashToken(rawToken), AuthTokenType.ACTIVATION))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.activate(new ActivateRequestDTO(rawToken)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_OR_EXPIRED_TOKEN);
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void loginConCuentaPendienteDevuelveAccountNotActivated() {
        User user = createUser("pendiente@uade.edu.ar", false);
        when(userRepository.findByEmailIgnoreCase("pendiente@uade.edu.ar")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secreta123", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("pendiente@uade.edu.ar", "Secreta123")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVATED);
    }

    @Test
    void loginConContrasenaIncorrectaDevuelveInvalidCredentials() {
        User user = createUser("activa@uade.edu.ar", true);
        when(userRepository.findByEmailIgnoreCase("activa@uade.edu.ar")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("MalaClave9", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("activa@uade.edu.ar", "MalaClave9")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginExitosoDevuelveTokenYPerfil() {
        User user = createUser("activa@uade.edu.ar", true);
        Instant expiresAt = NOW.plus(Duration.ofDays(30));
        when(userRepository.findByEmailIgnoreCase("activa@uade.edu.ar")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secreta123", user.getPasswordHash())).thenReturn(true);
        when(jwtService.issue(user.getId())).thenReturn(new JwtService.IssuedToken("jwt-activo", expiresAt));

        AuthResponseDTO response = authService.login(new LoginRequestDTO("ACTIVA@uade.edu.ar", "Secreta123"));

        assertThat(response.accessToken()).isEqualTo("jwt-activo");
        assertThat(response.user().id()).isEqualTo(user.getId());
    }

    @Test
    void recuperarContrasenaNoRevelaSiElCorreoExiste() {
        when(userRepository.findByEmailIgnoreCase("inexistente@uade.edu.ar")).thenReturn(Optional.empty());

        MessageResponseDTO response = authService.requestPasswordReset(
                new PasswordResetRequestDTO("inexistente@uade.edu.ar")
        );

        assertThat(response.message()).contains("Si el correo está registrado");
        verifyNoInteractions(authTokenRepository, authMailService);
    }

    @Test
    void reenviarActivacionReemplazaTokensAnterioresSoloSiEstaPendiente() {
        User pending = createUser("pendiente@uade.edu.ar", false);
        when(userRepository.findByEmailIgnoreCase("pendiente@uade.edu.ar")).thenReturn(Optional.of(pending));

        authService.resendActivation(new ResendActivationRequestDTO("pendiente@uade.edu.ar"));

        verify(authTokenRepository).deleteByUserIdAndType(pending.getId(), AuthTokenType.ACTIVATION);
        verify(authTokenRepository).save(any(AuthToken.class));
        verify(authMailService).sendActivationEmail(eq("pendiente@uade.edu.ar"), eq(pending.getFullName()), any());
    }

    @Test
    void confirmarRestablecimientoCambiaContrasenaYMarcaTokenUsado() {
        User user = createUser("activa@uade.edu.ar", true);
        String rawToken = "reset-token-123";
        AuthToken token = new AuthToken(
                AuthService.hashToken(rawToken),
                user,
                AuthTokenType.PASSWORD_RESET,
                NOW.plus(Duration.ofHours(1)),
                NOW
        );
        when(authTokenRepository.findByTokenHashAndType(AuthService.hashToken(rawToken), AuthTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NuevaClave9")).thenReturn("$2a$10$nuevoHash");

        authService.confirmPasswordReset(new ConfirmPasswordResetRequestDTO(rawToken, "NuevaClave9"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$nuevoHash");
        assertThat(token.getUsedAt()).isEqualTo(NOW);
    }

    private User createUser(String email, boolean active) {
        UserProfile profile = new UserProfile(
                "Lucía Fernández",
                "Ingeniería en Informática",
                "+5491155550000",
                PreferredMode.AMBOS,
                null
        );
        User user = new User(email, "$2a$10$existingHash", profile, NOW);
        if (active) {
            user.activate(NOW);
        }
        return user;
    }
}

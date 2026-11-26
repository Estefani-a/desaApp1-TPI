package com.example.uniride.backend.service;

import com.example.uniride.backend.TestData;
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
import com.example.uniride.backend.repository.AuthTokenRepository;
import com.example.uniride.backend.repository.UserRepository;
import com.example.uniride.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static com.example.uniride.backend.TestData.CLOCK;
import static com.example.uniride.backend.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthTokenRepository authTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                authTokenRepository,
                passwordEncoder,
                jwtService,
                eventPublisher,
                CLOCK
        );
    }

    @Test
    void registroConCorreoUadeCreaCuentaPendienteYPublicaEventoDeCorreo() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "  Lucia.F@UADE.EDU.AR ",
                "ñandú123",
                "  Lucía Fernández  ",
                "  Ingeniería en Informática ",
                " +5491155550000 ",
                PreferredMode.AMBOS,
                new VehicleDTO(" Volkswagen ", " Gol Trend ", " Gris ", " ab123cd ")
        );
        when(userRepository.findByEmailIgnoreCase("lucia.f@uade.edu.ar")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("ñandú123")).thenReturn("$2a$10$hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponseDTO response = authService.register(request);

        assertThat(response.message()).contains("activar");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("lucia.f@uade.edu.ar");
        assertThat(saved.getFullName()).isEqualTo("Lucía Fernández");
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.PENDIENTE);
        assertThat(saved.getVehicle().getPlate()).isEqualTo("AB123CD");

        verify(authTokenRepository).save(any(AuthToken.class));
        ArgumentCaptor<AuthEmailEvent> eventCaptor = ArgumentCaptor.forClass(AuthEmailEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(AuthTokenType.ACTIVATION);
        assertThat(eventCaptor.getValue().toEmail()).isEqualTo("lucia.f@uade.edu.ar");
    }

    @Test
    void registroConDominioExternoDevuelveInvalidEmailDomain() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "usuario@gmail.com", "Secreta123", "Juan Pérez", "Ingeniería",
                "+5491155550000", PreferredMode.PASAJERO, null
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_EMAIL_DOMAIN);
        verifyNoInteractions(userRepository, eventPublisher);
    }

    @Test
    void registroConContrasenaDebilDevuelveWeakPassword() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "usuario@uade.edu.ar", "sololetas", "Juan Pérez", "Ingeniería",
                "+5491155550000", PreferredMode.PASAJERO, null
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.WEAK_PASSWORD);
        verifyNoInteractions(userRepository, eventPublisher);
    }

    @Test
    void registroConContrasenaMayorA72BytesUtf8DevuelveWeakPassword() {
        // 37 'ñ' (74 bytes UTF-8) + '1' = 75 bytes > límite 72 bytes de BCrypt
        String longMultibytePassword = "ñ".repeat(37) + "1";
        RegisterRequestDTO request = new RegisterRequestDTO(
                "usuario@uade.edu.ar", longMultibytePassword, "Juan Pérez", "Ingeniería",
                "+5491155550000", PreferredMode.PASAJERO, null
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.WEAK_PASSWORD);
        verifyNoInteractions(userRepository, eventPublisher);
    }

    @Test
    void registroConCorreoActivoDuplicadoDevuelveEmailAlreadyRegistered() {
        User active = TestData.driver();
        active.activate(NOW);
        RegisterRequestDTO request = new RegisterRequestDTO(
                active.getEmail(), "Secreta123", "Lucía Fernández", "Ingeniería",
                "+5491155550000", PreferredMode.PASAJERO, null
        );
        when(userRepository.findByEmailIgnoreCase(active.getEmail())).thenReturn(Optional.of(active));
        when(passwordEncoder.encode("Secreta123")).thenReturn("$2a$10$hash");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void registroSobreCuentaPendienteSobreescribeDatosYReemiteToken() {
        User pending = TestData.passenger();
        RegisterRequestDTO request = new RegisterRequestDTO(
                pending.getEmail(), "NuevaClave9", "Martín Real", "Sistemas",
                "+5491199998888", PreferredMode.PASAJERO, null
        );
        when(userRepository.findByEmailIgnoreCase(pending.getEmail())).thenReturn(Optional.of(pending));
        when(passwordEncoder.encode("NuevaClave9")).thenReturn("$2a$10$nuevaHash");
        when(userRepository.saveAndFlush(pending)).thenReturn(pending);

        authService.register(request);

        assertThat(pending.getFullName()).isEqualTo("Martín Real");
        assertThat(pending.getPasswordHash()).isEqualTo("$2a$10$nuevaHash");
        verify(authTokenRepository).deleteByUserIdAndType(pending.getId(), AuthTokenType.ACTIVATION);
        verify(authTokenRepository).save(any(AuthToken.class));
        verify(eventPublisher).publishEvent(any(AuthEmailEvent.class));
    }

    @Test
    void registroSimultaneoTraduceDataIntegrityViolationAEmailAlreadyRegisteredSinEnviarCorreo() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "carrera@uade.edu.ar", "Secreta123", "Juan Pérez", "Ingeniería",
                "+5491155550000", PreferredMode.PASAJERO, null
        );
        when(userRepository.findByEmailIgnoreCase("carrera@uade.edu.ar")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secreta123")).thenReturn("$2a$10$hash");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
        verify(authTokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void activarCuentaConTokenValidoActivaYDevuelveJwt() {
        User user = TestData.driver();
        String rawToken = "token-valido-123";
        AuthToken token = new AuthToken(AuthService.hashToken(rawToken), user, AuthTokenType.ACTIVATION, NOW);
        Instant expiresAt = NOW.plus(Duration.ofDays(30));
        when(authTokenRepository.findByTokenHashAndTypeForUpdate(
                AuthService.hashToken(rawToken), AuthTokenType.ACTIVATION))
                .thenReturn(Optional.of(token));
        when(jwtService.issue(user.getId())).thenReturn(new JwtService.IssuedToken("jwt-token", expiresAt));

        AuthResponseDTO response = authService.activate(new ActivateRequestDTO(rawToken));

        assertThat(user.isActive()).isTrue();
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(response.user().email()).isEqualTo(user.getEmail());
    }

    @Test
    void activarCuentaConTokenVencidoDevuelveInvalidOrExpiredToken() {
        User user = TestData.driver();
        String rawToken = "token-vencido";
        AuthToken expired = new AuthToken(
                AuthService.hashToken(rawToken), user, AuthTokenType.ACTIVATION, NOW.minus(Duration.ofHours(25)));
        when(authTokenRepository.findByTokenHashAndTypeForUpdate(
                AuthService.hashToken(rawToken), AuthTokenType.ACTIVATION))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.activate(new ActivateRequestDTO(rawToken)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_OR_EXPIRED_TOKEN);
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void reenviarActivacionReemplazaTokensAnterioresCuandoEstaPendiente() {
        User pending = TestData.passenger();
        when(userRepository.findByEmailIgnoreCase(pending.getEmail())).thenReturn(Optional.of(pending));

        authService.resendActivation(new ResendActivationRequestDTO(pending.getEmail()));

        verify(authTokenRepository).deleteByUserIdAndType(pending.getId(), AuthTokenType.ACTIVATION);
        verify(authTokenRepository).save(any(AuthToken.class));
        verify(eventPublisher).publishEvent(any(AuthEmailEvent.class));
    }

    @Test
    void reenviarActivacionNoHaceNadaSiLaCuentaYaEstaActiva() {
        User active = TestData.driver();
        active.activate(NOW);
        when(userRepository.findByEmailIgnoreCase(active.getEmail())).thenReturn(Optional.of(active));

        authService.resendActivation(new ResendActivationRequestDTO(active.getEmail()));

        verifyNoInteractions(authTokenRepository, eventPublisher);
    }

    @Test
    void loginConCuentaPendienteDevuelveAccountNotActivated() {
        User pending = TestData.passenger();
        when(userRepository.findByEmailIgnoreCase(pending.getEmail())).thenReturn(Optional.of(pending));
        when(passwordEncoder.matches("Secreta123", pending.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO(pending.getEmail(), "Secreta123")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVATED);
    }

    @Test
    void loginConContrasenaIncorrectaDevuelveInvalidCredentials() {
        User active = TestData.driver();
        active.activate(NOW);
        when(userRepository.findByEmailIgnoreCase(active.getEmail())).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("MalaClave9", active.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO(active.getEmail(), "MalaClave9")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginConContrasenaMayorA72BytesDevuelveInvalidCredentialsSinError500() {
        User active = TestData.driver();
        active.activate(NOW);
        when(userRepository.findByEmailIgnoreCase(active.getEmail())).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO(active.getEmail(), "a".repeat(80) + "1")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void loginExitosoDevuelveTokenYPerfil() {
        User active = TestData.driver();
        active.activate(NOW);
        Instant expiresAt = NOW.plus(Duration.ofDays(30));
        when(userRepository.findByEmailIgnoreCase(active.getEmail())).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("Secreta123", active.getPasswordHash())).thenReturn(true);
        when(jwtService.issue(active.getId())).thenReturn(new JwtService.IssuedToken("jwt-activo", expiresAt));

        AuthResponseDTO response = authService.login(new LoginRequestDTO(active.getEmail().toUpperCase(), "Secreta123"));

        assertThat(response.accessToken()).isEqualTo("jwt-activo");
        assertThat(response.user().id()).isEqualTo(active.getId());
    }

    @Test
    void recuperarContrasenaDevuelveLaMismaRespuestaParaCorreoExistenteEInexistente() {
        User active = TestData.driver();
        active.activate(NOW);
        when(userRepository.findByEmailIgnoreCase("inexistente@uade.edu.ar")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(active.getEmail())).thenReturn(Optional.of(active));

        MessageResponseDTO missingResponse = authService.requestPasswordReset(
                new PasswordResetRequestDTO("inexistente@uade.edu.ar"));
        MessageResponseDTO activeResponse = authService.requestPasswordReset(
                new PasswordResetRequestDTO(active.getEmail()));

        assertThat(missingResponse.message()).isEqualTo(activeResponse.message());
        verify(authTokenRepository).deleteByUserIdAndType(active.getId(), AuthTokenType.PASSWORD_RESET);
        verify(authTokenRepository).save(any(AuthToken.class));
        verify(eventPublisher).publishEvent(any(AuthEmailEvent.class));
    }

    @Test
    void confirmarRestablecimientoCambiaContrasenaYMarcaTokenUsado() {
        User active = TestData.driver();
        active.activate(NOW);
        String rawToken = "reset-token-123";
        AuthToken token = new AuthToken(AuthService.hashToken(rawToken), active, AuthTokenType.PASSWORD_RESET, NOW);
        when(authTokenRepository.findByTokenHashAndTypeForUpdate(
                AuthService.hashToken(rawToken), AuthTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NuevaClave9")).thenReturn("$2a$10$nuevoHash");

        authService.confirmPasswordReset(new ConfirmPasswordResetRequestDTO(rawToken, "NuevaClave9"));

        assertThat(active.getPasswordHash()).isEqualTo("$2a$10$nuevoHash");
        assertThat(token.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    void confirmarRestablecimientoConTokenYaUsadoDevuelveInvalidOrExpiredToken() {
        User active = TestData.driver();
        active.activate(NOW);
        String rawToken = "reset-usado";
        AuthToken token = new AuthToken(AuthService.hashToken(rawToken), active, AuthTokenType.PASSWORD_RESET, NOW);
        token.consume(NOW.minusSeconds(10));
        when(authTokenRepository.findByTokenHashAndTypeForUpdate(
                AuthService.hashToken(rawToken), AuthTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.confirmPasswordReset(
                new ConfirmPasswordResetRequestDTO(rawToken, "NuevaClave9")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_OR_EXPIRED_TOKEN);
    }

    @Test
    void confirmarRestablecimientoConTokenVencidoDevuelveInvalidOrExpiredToken() {
        User active = TestData.driver();
        active.activate(NOW);
        String rawToken = "reset-vencido";
        AuthToken expired = new AuthToken(
                AuthService.hashToken(rawToken), active, AuthTokenType.PASSWORD_RESET, NOW.minus(Duration.ofHours(2)));
        when(authTokenRepository.findByTokenHashAndTypeForUpdate(
                AuthService.hashToken(rawToken), AuthTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.confirmPasswordReset(
                new ConfirmPasswordResetRequestDTO(rawToken, "NuevaClave9")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_OR_EXPIRED_TOKEN);
    }

    @Test
    void confirmarRestablecimientoConContrasenaDebilDevuelveWeakPassword() {
        assertThatThrownBy(() -> authService.confirmPasswordReset(
                new ConfirmPasswordResetRequestDTO("reset-token-123", "sinnumeros")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.WEAK_PASSWORD);
        verifyNoInteractions(authTokenRepository, userRepository);
    }
}

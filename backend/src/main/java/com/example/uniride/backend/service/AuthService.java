package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.ActivateRequestDTO;
import com.example.uniride.backend.dto.AuthResponseDTO;
import com.example.uniride.backend.dto.ConfirmPasswordResetRequestDTO;
import com.example.uniride.backend.dto.LoginRequestDTO;
import com.example.uniride.backend.dto.MessageResponseDTO;
import com.example.uniride.backend.dto.PasswordResetRequestDTO;
import com.example.uniride.backend.dto.RegisterRequestDTO;
import com.example.uniride.backend.dto.ResendActivationRequestDTO;
import com.example.uniride.backend.dto.UserResponseDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.AuthToken;
import com.example.uniride.backend.model.AuthTokenType;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.model.UserProfile;
import com.example.uniride.backend.repository.AuthTokenRepository;
import com.example.uniride.backend.repository.UserRepository;
import com.example.uniride.backend.security.JwtService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Business rules for RF01 authentication flows (docs/API.md §4.1–§4.5).
 */
@Service
public class AuthService {

    private static final String ALLOWED_DOMAIN = "uade.edu.ar";
    private static final int MIN_PASSWORD_LENGTH = 8;
    /** BCrypt rejects inputs longer than 72 UTF-8 bytes with IllegalArgumentException. */
    private static final int MAX_PASSWORD_BYTES = 72;

    private static final String REGISTER_MESSAGE = "Te enviamos un correo para activar la cuenta.";
    private static final String PASSWORD_RESET_MESSAGE =
            "Si el correo está registrado, te enviamos un enlace para restablecer la contraseña.";

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            AuthTokenRepository authTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public MessageResponseDTO register(RegisterRequestDTO request) {
        String email = normalizeEmail(request.email());
        validateInstitutionalEmailDomain(email);
        validatePasswordStrength(request.password());

        Instant now = clock.instant();
        UserProfile profile = request.toProfile();
        String passwordHash = passwordEncoder.encode(request.password());

        User user;
        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User current = existing.get();
            if (current.isActive()) {
                throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
            }
            // Overwrite unactivated account so nobody can hijack an email before the real owner registers.
            current.reregisterPending(passwordHash, profile, now);
            authTokenRepository.deleteByUserIdAndType(current.getId(), AuthTokenType.ACTIVATION);
            user = saveUserOrConflict(current);
        } else {
            user = saveUserOrConflict(new User(email, passwordHash, profile, now));
        }

        String rawToken = issueToken(user, AuthTokenType.ACTIVATION, now);
        eventPublisher.publishEvent(
                new AuthEmailEvent(AuthTokenType.ACTIVATION, user.getEmail(), user.getFullName(), rawToken)
        );

        return new MessageResponseDTO(REGISTER_MESSAGE);
    }

    @Transactional
    public AuthResponseDTO activate(ActivateRequestDTO request) {
        Instant now = clock.instant();
        AuthToken token = consumeToken(request.token(), AuthTokenType.ACTIVATION, now);

        User user = token.getUser();
        user.activate(now);
        authTokenRepository.save(token);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public void resendActivation(ResendActivationRequestDTO request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> !user.isActive())
                .ifPresent(user -> {
                    Instant now = clock.instant();
                    authTokenRepository.deleteByUserIdAndType(user.getId(), AuthTokenType.ACTIVATION);
                    String rawToken = issueToken(user, AuthTokenType.ACTIVATION, now);
                    eventPublisher.publishEvent(
                            new AuthEmailEvent(AuthTokenType.ACTIVATION, user.getEmail(), user.getFullName(), rawToken)
                    );
                });
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (exceedsBcryptLimit(request.password())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public MessageResponseDTO requestPasswordReset(PasswordResetRequestDTO request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .ifPresent(user -> {
                    Instant now = clock.instant();
                    authTokenRepository.deleteByUserIdAndType(user.getId(), AuthTokenType.PASSWORD_RESET);
                    String rawToken = issueToken(user, AuthTokenType.PASSWORD_RESET, now);
                    eventPublisher.publishEvent(
                            new AuthEmailEvent(
                                    AuthTokenType.PASSWORD_RESET, user.getEmail(), user.getFullName(), rawToken)
                    );
                });

        return new MessageResponseDTO(PASSWORD_RESET_MESSAGE);
    }

    @Transactional
    public void confirmPasswordReset(ConfirmPasswordResetRequestDTO request) {
        validatePasswordStrength(request.newPassword());

        Instant now = clock.instant();
        AuthToken token = consumeToken(request.token(), AuthTokenType.PASSWORD_RESET, now);

        User user = token.getUser();
        user.changePassword(passwordEncoder.encode(request.newPassword()), now);
        authTokenRepository.save(token);
        userRepository.save(user);
    }

    private User saveUserOrConflict(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        JwtService.IssuedToken issued = jwtService.issue(user.getId());
        return new AuthResponseDTO(issued.accessToken(), issued.expiresAt(), UserResponseDTO.from(user));
    }

    private String issueToken(User user, AuthTokenType type, Instant now) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthToken entity = new AuthToken(hashToken(rawToken), user, type, now);
        authTokenRepository.save(entity);
        return rawToken;
    }

    private AuthToken consumeToken(String rawToken, AuthTokenType type, Instant now) {
        String tokenHash = hashToken(rawToken.trim());
        AuthToken token = authTokenRepository.findByTokenHashAndTypeForUpdate(tokenHash, type)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_OR_EXPIRED_TOKEN));
        token.consume(now);
        return token;
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private void validateInstitutionalEmailDomain(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex < 0 || !ALLOWED_DOMAIN.equals(email.substring(atIndex + 1))) {
            throw new ApiException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }
    }

    /** Matches Android PasswordPolicy: min 8 chars, max 72 UTF-8 bytes (BCrypt), >=1 Unicode letter and >=1 ASCII digit. */
    private void validatePasswordStrength(String password) {
        if (password == null || password.isBlank()
                || password.length() < MIN_PASSWORD_LENGTH
                || exceedsBcryptLimit(password)) {
            throw new ApiException(ErrorCode.WEAK_PASSWORD);
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isLetter(ch)) {
                hasLetter = true;
            } else if (ch >= '0' && ch <= '9') {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new ApiException(ErrorCode.WEAK_PASSWORD);
        }
    }

    private boolean exceedsBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}

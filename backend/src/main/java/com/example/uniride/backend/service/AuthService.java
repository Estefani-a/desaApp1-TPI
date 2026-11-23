package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.ActivateRequest;
import com.example.uniride.backend.dto.AuthResponse;
import com.example.uniride.backend.dto.ConfirmPasswordResetRequest;
import com.example.uniride.backend.dto.LoginRequest;
import com.example.uniride.backend.dto.MessageResponse;
import com.example.uniride.backend.dto.PasswordResetRequest;
import com.example.uniride.backend.dto.RegisterRequest;
import com.example.uniride.backend.dto.ResendActivationRequest;
import com.example.uniride.backend.dto.UserResponse;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.AuthToken;
import com.example.uniride.backend.model.AuthTokenType;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.model.UserProfile;
import com.example.uniride.backend.model.Vehicle;
import com.example.uniride.backend.repository.AuthTokenRepository;
import com.example.uniride.backend.repository.UserRepository;
import com.example.uniride.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Business rules for RF01 authentication flows (docs/API.md §4.1–§4.5).
 */
@Service
public class AuthService {

    private static final String ALLOWED_DOMAIN = "uade.edu.ar";
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Duration ACTIVATION_TTL = Duration.ofHours(24);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofHours(1);
    private static final Pattern EMAIL_FORMAT = Pattern.compile(
            "^[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$"
    );

    private static final String REGISTER_MESSAGE = "Te enviamos un correo para activar la cuenta.";
    private static final String PASSWORD_RESET_MESSAGE =
            "Si el correo está registrado, te enviamos un enlace para restablecer la contraseña.";

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMailService authMailService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            AuthTokenRepository authTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthMailService authMailService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authMailService = authMailService;
        this.clock = clock;
    }

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validateInstitutionalEmail(email);
        validatePasswordStrength(request.password());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        Instant now = clock.instant();
        Vehicle vehicle = request.vehicle() != null ? request.vehicle().toEntity() : null;
        UserProfile profile = new UserProfile(
                request.fullName().trim(),
                request.career().trim(),
                request.phone().trim(),
                request.preferredMode(),
                vehicle
        );
        User user = new User(email, passwordEncoder.encode(request.password()), profile, now);
        userRepository.save(user);

        String rawToken = issueToken(user, AuthTokenType.ACTIVATION, ACTIVATION_TTL, now);
        authMailService.sendActivationEmail(user.getEmail(), user.getFullName(), rawToken);

        return new MessageResponse(REGISTER_MESSAGE);
    }

    @Transactional
    public AuthResponse activate(ActivateRequest request) {
        Instant now = clock.instant();
        AuthToken token = findUsableToken(request.token(), AuthTokenType.ACTIVATION, now);

        User user = token.getUser();
        token.markUsed(now);
        user.activate(now);
        authTokenRepository.save(token);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public void resendActivation(ResendActivationRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> !user.isActive())
                .ifPresent(user -> {
                    Instant now = clock.instant();
                    authTokenRepository.deleteByUserIdAndType(user.getId(), AuthTokenType.ACTIVATION);
                    String rawToken = issueToken(user, AuthTokenType.ACTIVATION, ACTIVATION_TTL, now);
                    authMailService.sendActivationEmail(user.getEmail(), user.getFullName(), rawToken);
                });
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public MessageResponse requestPasswordReset(PasswordResetRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .ifPresent(user -> {
                    Instant now = clock.instant();
                    authTokenRepository.deleteByUserIdAndType(user.getId(), AuthTokenType.PASSWORD_RESET);
                    String rawToken = issueToken(user, AuthTokenType.PASSWORD_RESET, PASSWORD_RESET_TTL, now);
                    authMailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
                });

        return new MessageResponse(PASSWORD_RESET_MESSAGE);
    }

    @Transactional
    public void confirmPasswordReset(ConfirmPasswordResetRequest request) {
        validatePasswordStrength(request.newPassword());

        Instant now = clock.instant();
        AuthToken token = findUsableToken(request.token(), AuthTokenType.PASSWORD_RESET, now);

        User user = token.getUser();
        token.markUsed(now);
        user.changePassword(passwordEncoder.encode(request.newPassword()), now);
        authTokenRepository.save(token);
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        JwtService.IssuedToken issued = jwtService.issue(user.getId());
        return new AuthResponse(issued.accessToken(), issued.expiresAt(), UserResponse.from(user));
    }

    private String issueToken(User user, AuthTokenType type, Duration ttl, Instant now) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthToken entity = new AuthToken(hashToken(rawToken), user, type, now.plus(ttl), now);
        authTokenRepository.save(entity);
        return rawToken;
    }

    private AuthToken findUsableToken(String rawToken, AuthTokenType type, Instant now) {
        String tokenHash = hashToken(rawToken.trim());
        AuthToken token = authTokenRepository.findByTokenHashAndType(tokenHash, type)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_OR_EXPIRED_TOKEN));
        if (!token.isUsable(now)) {
            throw new ApiException(ErrorCode.INVALID_OR_EXPIRED_TOKEN);
        }
        return token;
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private void validateInstitutionalEmail(String email) {
        if (!EMAIL_FORMAT.matcher(email).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "El correo electrónico no tiene un formato válido.");
        }
        String domain = email.substring(email.indexOf('@') + 1);
        if (!ALLOWED_DOMAIN.equals(domain)) {
            throw new ApiException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }
    }

    /** Matches Android PasswordPolicy: min 8 chars, at least one Unicode letter and one ASCII digit. */
    private void validatePasswordStrength(String password) {
        if (password == null || password.isBlank() || password.length() < MIN_PASSWORD_LENGTH) {
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

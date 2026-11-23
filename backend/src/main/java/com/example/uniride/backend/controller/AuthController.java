package com.example.uniride.backend.controller;

import com.example.uniride.backend.dto.ActivateRequest;
import com.example.uniride.backend.dto.AuthResponse;
import com.example.uniride.backend.dto.ConfirmPasswordResetRequest;
import com.example.uniride.backend.dto.LoginRequest;
import com.example.uniride.backend.dto.MessageResponse;
import com.example.uniride.backend.dto.PasswordResetRequest;
import com.example.uniride.backend.dto.RegisterRequest;
import com.example.uniride.backend.dto.ResendActivationRequest;
import com.example.uniride.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication endpoints for RF01 (docs/API.md §4.1–§4.5). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/activate")
    public AuthResponse activate(@Valid @RequestBody ActivateRequest request) {
        return authService.activate(request);
    }

    @PostMapping("/resend-activation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendActivation(@Valid @RequestBody ResendActivationRequest request) {
        authService.resendActivation(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetRequest request) {
        authService.confirmPasswordReset(request);
    }
}

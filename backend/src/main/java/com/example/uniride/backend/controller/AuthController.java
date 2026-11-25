package com.example.uniride.backend.controller;

import com.example.uniride.backend.dto.ActivateRequestDTO;
import com.example.uniride.backend.dto.AuthResponseDTO;
import com.example.uniride.backend.dto.ConfirmPasswordResetRequestDTO;
import com.example.uniride.backend.dto.LoginRequestDTO;
import com.example.uniride.backend.dto.MessageResponseDTO;
import com.example.uniride.backend.dto.PasswordResetRequestDTO;
import com.example.uniride.backend.dto.RegisterRequestDTO;
import com.example.uniride.backend.dto.ResendActivationRequestDTO;
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
    public MessageResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
        return authService.register(request);
    }

    @PostMapping("/activate")
    public AuthResponseDTO activate(@Valid @RequestBody ActivateRequestDTO request) {
        return authService.activate(request);
    }

    @PostMapping("/resend-activation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendActivation(@Valid @RequestBody ResendActivationRequestDTO request) {
        authService.resendActivation(request);
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponseDTO requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetRequestDTO request) {
        authService.confirmPasswordReset(request);
    }
}

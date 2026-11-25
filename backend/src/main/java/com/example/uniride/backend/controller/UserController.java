package com.example.uniride.backend.controller;

import com.example.uniride.backend.dto.DeviceTokenRequestDTO;
import com.example.uniride.backend.dto.UpdateProfileRequestDTO;
import com.example.uniride.backend.dto.UserResponseDTO;
import com.example.uniride.backend.security.CurrentUser;
import com.example.uniride.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated user profile and device-token endpoints for RF01 (docs/API.md §4.6). */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public UserResponseDTO getProfile() {
        return userService.getProfile(currentUser.id());
    }

    @PutMapping("/me")
    public UserResponseDTO updateProfile(@Valid @RequestBody UpdateProfileRequestDTO request) {
        return userService.updateProfile(currentUser.id(), request);
    }

    @PutMapping("/me/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDeviceToken(@Valid @RequestBody DeviceTokenRequestDTO request) {
        userService.updateDeviceToken(currentUser.id(), request.fcmToken());
    }

    @DeleteMapping("/me/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeviceToken() {
        userService.deleteDeviceToken(currentUser.id());
    }
}

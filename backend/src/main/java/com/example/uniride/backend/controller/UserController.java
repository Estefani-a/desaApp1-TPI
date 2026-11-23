package com.example.uniride.backend.controller;

import com.example.uniride.backend.dto.DeviceTokenRequest;
import com.example.uniride.backend.dto.UpdateProfileRequest;
import com.example.uniride.backend.dto.UserResponse;
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
    public UserResponse getProfile() {
        return userService.getProfile(currentUser.id());
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUser.id(), request);
    }

    @PutMapping("/me/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDeviceToken(@Valid @RequestBody DeviceTokenRequest request) {
        userService.updateDeviceToken(currentUser.id(), request.fcmToken());
    }

    @DeleteMapping("/me/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeviceToken() {
        userService.deleteDeviceToken(currentUser.id());
    }
}

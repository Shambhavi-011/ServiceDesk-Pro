package com.servicedeskpro.controller;

import com.servicedeskpro.dto.request.LoginRequestDto;
import com.servicedeskpro.dto.request.RefreshTokenRequestDto;
import com.servicedeskpro.dto.request.RegisterRequestDto;
import com.servicedeskpro.dto.response.ApiResponse;
import com.servicedeskpro.dto.response.JwtAuthResponseDto;
import com.servicedeskpro.dto.response.UserProfileDto;
import com.servicedeskpro.dto.response.UserResponseDto;
import com.servicedeskpro.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Identity", description = "Endpoints for user registration, login, JWT token rotation, and profile")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new Employee account", description = "Allows self-registration for standard employees")
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        UserResponseDto user = authService.register(request);
        return new ResponseEntity<>(ApiResponse.success(user, "User registration successful"), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates user credentials and issues Access Token (JWT) + Refresh Token")
    public ResponseEntity<ApiResponse<JwtAuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        JwtAuthResponseDto authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate & Refresh JWT Access Token", description = "Validates refresh token and returns a fresh JWT access token")
    public ResponseEntity<ApiResponse<JwtAuthResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        JwtAuthResponseDto authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Logout User", description = "Revokes the active refresh token session")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequestDto request) {
        if (request != null) {
            authService.logout(request.getRefreshToken());
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get Authenticated User Profile", description = "Returns the profile of the currently logged-in user")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser() {
        UserProfileDto profile = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully"));
    }
}
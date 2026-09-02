package com.servicedeskpro.service;

import com.servicedeskpro.dto.request.LoginRequestDto;
import com.servicedeskpro.dto.request.RefreshTokenRequestDto;
import com.servicedeskpro.dto.request.RegisterRequestDto;
import com.servicedeskpro.dto.response.JwtAuthResponseDto;
import com.servicedeskpro.dto.response.UserProfileDto;
import com.servicedeskpro.dto.response.UserResponseDto;

public interface AuthService {
    UserResponseDto register(RegisterRequestDto request);
    JwtAuthResponseDto login(LoginRequestDto request);
    JwtAuthResponseDto refreshToken(RefreshTokenRequestDto request);
    void logout(String refreshToken);
    UserProfileDto getCurrentUserProfile();
}
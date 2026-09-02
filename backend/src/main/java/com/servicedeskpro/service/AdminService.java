package com.servicedeskpro.service;

import com.servicedeskpro.dto.request.CreateAgentRequestDto;
import com.servicedeskpro.dto.request.UpdateUserStatusDto;
import com.servicedeskpro.dto.response.PageResponse;
import com.servicedeskpro.dto.response.UserResponseDto;

public interface AdminService {
    UserResponseDto createSupportAgent(CreateAgentRequestDto request);
    PageResponse<UserResponseDto> getAllUsers(int page, int size, Boolean activeOnly);
    void updateUserStatus(Long userId, UpdateUserStatusDto request);
}
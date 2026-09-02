package com.servicedeskpro.service.impl;

import com.servicedeskpro.dto.request.CreateAgentRequestDto;
import com.servicedeskpro.dto.request.UpdateUserStatusDto;
import com.servicedeskpro.dto.response.PageResponse;
import com.servicedeskpro.dto.response.UserResponseDto;
import com.servicedeskpro.entity.Role;
import com.servicedeskpro.entity.User;
import com.servicedeskpro.entity.enums.RoleName;
import com.servicedeskpro.exception.BadRequestException;
import com.servicedeskpro.exception.ResourceNotFoundException;
import com.servicedeskpro.repository.RoleRepository;
import com.servicedeskpro.repository.UserRepository;
import com.servicedeskpro.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponseDto createSupportAgent(CreateAgentRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        Role agentRole = roleRepository.findByName(RoleName.ROLE_SUPPORT_AGENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_SUPPORT_AGENT).build()));

        User agent = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .department(request.getDepartment())
                .active(true)
                .roles(Set.of(agentRole))
                .build();

        return mapToDto(userRepository.save(agent));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponseDto> getAllUsers(int page, int size, Boolean activeOnly) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;
        if (Boolean.TRUE.equals(activeOnly)) {
            userPage = userRepository.findAllByActive(true, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }
        return PageResponse.of(userPage.map(this::mapToDto));
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, UpdateUserStatusDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setActive(request.getActive());
        userRepository.save(user);
    }

    private UserResponseDto mapToDto(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .department(user.getDepartment())
                .active(user.isActive())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
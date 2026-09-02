package com.servicedeskpro.controller;

import com.servicedeskpro.dto.request.CreateAgentRequestDto;
import com.servicedeskpro.dto.request.UpdateUserStatusDto;
import com.servicedeskpro.dto.response.ApiResponse;
import com.servicedeskpro.dto.response.AuditLogDto;
import com.servicedeskpro.dto.response.PageResponse;
import com.servicedeskpro.dto.response.UserResponseDto;
import com.servicedeskpro.service.AdminService;
import com.servicedeskpro.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administration", description = "Admin-only endpoints for user governance, agent provisioning, and audit logs")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @PostMapping("/agents")
    @Operation(summary = "Provision Support Agent (Admin Only)", description = "Creates a new Support Agent user profile")
    public ResponseEntity<ApiResponse<UserResponseDto>> createAgent(@Valid @RequestBody CreateAgentRequestDto request) {
        UserResponseDto created = adminService.createSupportAgent(request);
        return new ResponseEntity<>(ApiResponse.success(created, "Support agent provisioned successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/users")
    @Operation(summary = "List All Users (Admin Only)", description = "Retrieves paginated list of all system users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean activeOnly) {
        PageResponse<UserResponseDto> users = adminService.getAllUsers(page, size, activeOnly);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Update User Active Status (Admin Only)", description = "Activates or deactivates user account access")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusDto request) {
        adminService.updateUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "User status updated successfully"));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Inspect System Audit Logs (Admin Only)", description = "Retrieves immutable, paginated system audit ledger")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AuditLogDto> auditLogs = auditLogService.getAllAuditLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }
}
package com.servicedeskpro.controller;

import com.servicedeskpro.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Endpoints for verifying system health and version")
public class HealthCheckController {

    @GetMapping
    @Operation(summary = "System Health Check", description = "Returns operational health status and timestamp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> healthInfo = Map.of(
                "status", "UP",
                "service", "ServiceDesk Pro API",
                "version", "1.0.0-SNAPSHOT",
                "systemTime", System.currentTimeMillis()
        );
        return ResponseEntity.ok(ApiResponse.success(healthInfo, "ServiceDesk Pro backend is running seamlessly"));
    }
}

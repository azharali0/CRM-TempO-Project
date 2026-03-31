package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @GetMapping("/version")
    public ResponseEntity<ApiResponse<Map<String, String>>> getVersion() {
        return ResponseEntity.ok(ApiResponse.success("Version retrieved",
                Map.of("version", appVersion, "name", "CRM Desktop Application")));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Service is healthy",
                Map.of("status", "UP")));
    }
}

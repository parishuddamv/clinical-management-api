package com.clinicos.common.controller;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.common.dto.GoogleAuthRequest;
import com.clinicos.common.dto.GoogleAuthResponse;
import com.clinicos.common.service.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Google OAuth2 Authentication Controller
 * Handles Google sign-in and JWT token generation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    /**
     * POST /auth/google
     * Authenticate user with Google ID Token
     * Converts Google token to JWT token for API access
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<GoogleAuthResponse>> authenticateWithGoogle(
            @RequestBody GoogleAuthRequest request) {

        log.info("Received Google authentication request");

        GoogleAuthResponse response = googleAuthService.authenticateWithGoogle(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(
                        "Google authentication successful",
                        response
                ));
    }

    /**
     * GET /auth/google/config
     * Get Google OAuth2 configuration for frontend
     * Returns client ID, redirect URL, and required scopes
     */
    @GetMapping("/google/config")
    public ResponseEntity<ApiResponse<GoogleAuthService.GoogleAuthConfigResponse>> getGoogleConfig() {
        log.info("Returning Google OAuth2 configuration");

        GoogleAuthService.GoogleAuthConfigResponse config = googleAuthService.getGoogleConfig();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Google OAuth2 configuration retrieved",
                        config
                )
        );
    }

    /**
     * GET /auth/health
     * Health check endpoint for authentication service
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(
                ApiResponse.success("Authentication service is healthy")
        );
    }
}


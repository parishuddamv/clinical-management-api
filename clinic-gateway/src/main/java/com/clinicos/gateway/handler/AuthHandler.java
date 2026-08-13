package com.clinicos.gateway.handler;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.common.dto.ClinicUserResponse;
import com.clinicos.common.dto.GoogleAuthRequest;
import com.clinicos.common.dto.RegistrationRequest;
import com.clinicos.common.dto.UserStatusResponse;
import com.clinicos.common.service.GoogleAuthService;
import com.clinicos.common.service.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.ServerResponse.BodyBuilder;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reactive handler for authentication endpoints
 * Handles user registration, status checking, and admin operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandler {

    private final UserRegistrationService userRegistrationService;
    private final GoogleAuthService googleAuthService;
    private final ObjectMapper objectMapper;

    /**
     * Handle POST /api/v1/auth/register
     * Register a new user
     */
    public Mono<ServerResponse> registerUser(ServerRequest request) {
        return request.bodyToMono(RegistrationRequest.class)
                .flatMap(reqBody -> {
                    try {
                        log.info("Registering new user: {}", reqBody.getEmail());
                        ClinicUserResponse response = userRegistrationService.registerUser(reqBody);
                        return ServerResponse
                                .status(HttpStatus.CREATED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.success("Registration successful! Please wait for approval.", response));
                    } catch (IllegalArgumentException e) {
                        log.warn("Registration failed: {}", e.getMessage());
                        return ServerResponse
                                .status(HttpStatus.CONFLICT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.error(e.getMessage()));
                    } catch (Exception e) {
                        log.error("Registration error: {}", e.getMessage(), e);
                        return ServerResponse
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.error("Registration failed: " + e.getMessage()));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Request processing error: {}", e.getMessage());
                    return ServerResponse
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error("Request processing error: " + e.getMessage()));
                });
    }

    /**
     * Handle GET /api/v1/auth/google and GET /api/v1/auth/google/config
     * Returns Google OAuth config for the frontend.
     */
    public Mono<ServerResponse> getGoogleConfig(ServerRequest request) {
        try {
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success(googleAuthService.getGoogleConfig()));
        } catch (Exception e) {
            log.error("Error getting Google config: {}", e.getMessage(), e);
            return ServerResponse
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error("Failed to load Google auth config: " + e.getMessage()));
        }
    }

    /**
     * Handle POST /api/v1/auth/google
     * Authenticates a Google user using the posted ID token.
     */
    public Mono<ServerResponse> authenticateWithGoogle(ServerRequest request) {
        return request.bodyToMono(GoogleAuthRequest.class)
                .flatMap(reqBody -> {
                    try {
                        log.info("Authenticating Google user request");
                        return ServerResponse
                                .ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.success(googleAuthService.authenticateWithGoogle(reqBody)));
                    } catch (IllegalArgumentException e) {
                        log.warn("Google auth validation failed: {}", e.getMessage());
                        return ServerResponse
                                .status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.error(e.getMessage()));
                    } catch (Exception e) {
                        log.error("Google auth failed: {}", e.getMessage(), e);
                        return ServerResponse
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.error("Google authentication failed: " + e.getMessage()));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Request processing error for Google auth: {}", e.getMessage(), e);
                    return ServerResponse
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error("Request processing error: " + e.getMessage()));
                });
    }

    /**
     * Handle GET /api/v1/auth/user-status/{email}
     * Check user approval status
     */
    public Mono<ServerResponse> getUserStatus(ServerRequest request) {
        String email = request.pathVariable("email");
        log.info("Checking user status for: {}", email);

        try {
            UserStatusResponse response = userRegistrationService.getUserStatus(email);
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("User status check failed: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error checking user status: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error("Error checking status: " + e.getMessage()));
        }
    }

    /**
     * Handle GET /api/v1/auth/check-approval/{email}
     * Quick check if user is approved
     */
    public Mono<ServerResponse> checkApproval(ServerRequest request) {
        String email = request.pathVariable("email");
        log.info("Checking approval status for: {}", email);

        try {
            boolean isApproved = userRegistrationService.isUserApproved(email);
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success(isApproved));
        } catch (Exception e) {
            log.error("Approval check failed: {}", e.getMessage());
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success(false));
        }
    }

    /**
     * Handle GET /api/v1/auth/user/{email}
     * Get user details
     */
    public Mono<ServerResponse> getUser(ServerRequest request) {
        String email = request.pathVariable("email");
        log.info("Getting user: {}", email);

        try {
            ClinicUserResponse response = userRegistrationService.getUser(email);
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("Get user failed: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting user: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error("Error getting user: " + e.getMessage()));
        }
    }

    /**
     * Handle PUT /api/v1/auth/admin/approve/{email}
     * Approve a user registration (Admin only)
     */
    public Mono<ServerResponse> approveUser(ServerRequest request) {
        String email = request.pathVariable("email");
        String clinicId = request.queryParam("clinicId").orElse("");
        String approvedBy = request.queryParam("approvedBy").orElse("");

        log.info("Approving user: {}", email);

        if (clinicId.isEmpty() || approvedBy.isEmpty()) {
            return ServerResponse
                    .status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error("clinicId and approvedBy query parameters are required"));
        }

        try {
            ClinicUserResponse response = userRegistrationService.approveUser(email, clinicId, approvedBy);
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success("User approved successfully", response));
        } catch (IllegalArgumentException e) {
            log.warn("User approval failed: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error approving user: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error( "Error approving user: " + e.getMessage()));
        }
    }

    /**
     * Handle PUT /api/v1/auth/admin/reject/{email}
     * Reject a user registration (Admin only)
     */
    public Mono<ServerResponse> rejectUser(ServerRequest request) {
        String email = request.pathVariable("email");
        String rejectionReason = request.queryParam("rejectionReason").orElse("No reason provided");

        log.info("Rejecting user: {}", email);

        try {
            ClinicUserResponse response = userRegistrationService.rejectUser(email, rejectionReason);
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success("User rejected successfully", response));
        } catch (IllegalArgumentException e) {
            log.warn("User rejection failed: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error rejecting user: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error("Error rejecting user: " + e.getMessage()));
        }
    }

    /**
     * Handle PUT /api/v1/auth/admin/suspend/{email}
     * Suspend a user account (Admin only)
     */
    public Mono<ServerResponse> suspendUser(ServerRequest request) {
        String email = request.pathVariable("email");
        log.info("Suspending user: {}", email);

        try {
            ClinicUserResponse response = userRegistrationService.suspendUser(email);
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success("User suspended successfully", response));
        } catch (IllegalArgumentException e) {
            log.warn("User suspension failed: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error suspending user: {}", e.getMessage());
            return ServerResponse
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error("Error suspending user: " + e.getMessage()));
        }
    }
}


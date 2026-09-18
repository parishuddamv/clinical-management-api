package com.clinicos.gateway.handler;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.common.dto.ClinicUserResponse;
import com.clinicos.common.dto.GoogleAuthRequest;
import com.clinicos.common.dto.RegistrationRequest;
import com.clinicos.common.dto.UserStatusResponse;
import com.clinicos.common.service.GoogleAuthService;
import com.clinicos.common.service.UserRegistrationService;
import com.clinicos.common.exception.RegistrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
                .doOnError(e -> log.error("REGISTRATION BODY DESERIALIZATION ERROR", e))
                .switchIfEmpty(Mono.error(new RegistrationException(HttpStatus.BAD_REQUEST, "Registration body is required.")))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(reqBody -> {
                    try {
                        log.info("Registering new user: {}", reqBody.getEmail());
                        ClinicUserResponse response = userRegistrationService.registerUser(reqBody);
                        return ServerResponse
                                .status(HttpStatus.CREATED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.success("Registration successful. Your request is awaiting Super Admin approval.", response));
                    } catch (RegistrationException e) {
                        return error(e);
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
                                .bodyValue(ApiResponse.error("Registration could not be processed. Please try again later."));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Request processing error: {}", e.getMessage(), e);
                    return error(e);
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
                    .bodyValue(ApiResponse.error("Failed to load Google auth config."));
        }
    }

    /**
     * Handle POST /api/v1/auth/google
     * Authenticates a Google user using the posted ID token.
     */
    public Mono<ServerResponse> authenticateWithGoogle(ServerRequest request) {
        return request.bodyToMono(GoogleAuthRequest.class)
                .switchIfEmpty(Mono.error(new RegistrationException(HttpStatus.BAD_REQUEST, "Google authentication body is required.")))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(reqBody -> {
                    try {
                        log.info("Authenticating Google user request");
                        return ServerResponse
                                .ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.success(googleAuthService.authenticateWithGoogle(reqBody)));
                    } catch (RegistrationException e) {
                        return error(e);
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
                                .bodyValue(ApiResponse.error("Google authentication failed."));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Request processing error for Google auth: {}", e.getMessage(), e);
                    return error(e);
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
            UserStatusResponse response = userRegistrationService.getUserStatus(email, authorization(request));
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success(response));
        } catch (RegistrationException e) {
            return error(e);
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
                    .bodyValue(ApiResponse.error("Unable to check registration status."));
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
            boolean isApproved = userRegistrationService.isUserApproved(email, authorization(request));
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success(isApproved));
        } catch (Exception e) {
            return error(e);
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
            ClinicUserResponse response = userRegistrationService.getUser(email, authorization(request));
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success(response));
        } catch (RegistrationException e) {
            return error(e);
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
                    .bodyValue(ApiResponse.error("Unable to retrieve registration."));
        }
    }

    /**
     * Handle PUT /api/v1/auth/admin/approve/{email}
     * Approve a user registration (Admin only)
     */
    public Mono<ServerResponse> approveUser(ServerRequest request) {
        String email = request.pathVariable("email");
        String clinicId = request.queryParam("clinicId").orElse(null);

        log.info("Approving user: {}", email);

        try {
            ClinicUserResponse response = userRegistrationService.approveUser(email, clinicId, authorization(request));
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success("User approved successfully", response));
        } catch (RegistrationException e) {
            return error(e);
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
                    .bodyValue(ApiResponse.error("Unable to approve registration."));
        }
    }

    /**
     * Handle PUT /api/v1/auth/admin/reject/{email}
     * Reject a user registration (Admin only)
     */
    public Mono<ServerResponse> rejectUser(ServerRequest request) {
        String email = request.pathVariable("email");
        String rejectionReason = request.queryParam("rejectionReason").orElse(null);

        log.info("Rejecting user: {}", email);

        try {
            ClinicUserResponse response = userRegistrationService.rejectUser(email, rejectionReason, authorization(request));
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success("Registration rejected successfully.", response));
        } catch (RegistrationException e) {
            return error(e);
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
                    .bodyValue(ApiResponse.error("Unable to reject registration."));
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
            ClinicUserResponse response = userRegistrationService.suspendUser(email, authorization(request));
            return ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.success("User suspended successfully", response));
        } catch (RegistrationException e) {
            return error(e);
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
                    .bodyValue(ApiResponse.error("Unable to suspend user."));
        }
    }
    public Mono<ServerResponse> registrations(ServerRequest request) {
        return operation(() -> userRegistrationService.registrations(authorization(request),
                request.queryParam("status").orElse(null), request.queryParam("search").orElse(null),
                page(request, "page", 0), page(request, "size", 20)), "Success");
    }

    public Mono<ServerResponse> history(ServerRequest request) {
        return operation(() -> userRegistrationService.history(request.pathVariable("email"), authorization(request),
                page(request, "page", 0), page(request, "size", 20)), "Success");
    }

    public Mono<ServerResponse> reactivateUser(ServerRequest request) {
        return operation(() -> userRegistrationService.reactivateUser(request.pathVariable("email"), authorization(request)),
                "User reactivated successfully.");
    }

    public Mono<ServerResponse> submitForReview(ServerRequest request) {
        return operation(() -> userRegistrationService.submitForReview(request.pathVariable("email"), authorization(request)),
                "Registration submitted for review.");
    }

    private Mono<ServerResponse> operation(java.util.concurrent.Callable<?> action, String message) {
        return Mono.fromCallable(action).subscribeOn(Schedulers.boundedElastic())
                .flatMap(value -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponse.success(message, value)))
                .onErrorResume(this::error);
    }

    private String authorization(ServerRequest request) {
        return request.headers().firstHeader("Authorization");
    }

    private int page(ServerRequest request, String key, int fallback) {
        try {
            return Integer.parseInt(request.queryParam(key).orElse(String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters.");
        }
    }

    private Mono<ServerResponse> error(Throwable e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Request could not be processed. Please try again later.";
        if (e instanceof RegistrationException registration) {
            status = registration.getStatus();
            message = registration.getMessage();
        } else if (e instanceof org.springframework.web.server.ServerWebInputException
                || e instanceof org.springframework.core.codec.DecodingException) {
            status = HttpStatus.BAD_REQUEST;
            message = "Invalid request body.";
        } else {
            log.error("Request processing error: {}", e.getMessage(), e);
        }
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).bodyValue(ApiResponse.error(message));
    }
}


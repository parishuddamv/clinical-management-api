package com.clinicos.common.controller;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.common.dto.ClinicUserResponse;
import com.clinicos.common.dto.RegistrationRequest;
import com.clinicos.common.dto.UserStatusResponse;
import com.clinicos.common.service.UserRegistrationService;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.entity.RegistrationHistory;
import org.springframework.data.domain.Page;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User Registration Controller
 * Handles user registration, approval, and status checking endpoints
 */
@Slf4j
@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserRegistrationController {

    private final UserRegistrationService userRegistrationService;

    /**
     * POST /auth/register
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<ClinicUserResponse>> registerUser(
            @Valid @RequestBody RegistrationRequest request) {
        log.info("Registering new user: {}", request.getEmail());
        try {
            ClinicUserResponse response = userRegistrationService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Registration successful. Your request is awaiting Super Admin approval.", response));
        } catch (RegistrationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Registration could not be processed. Please try again later."));
        }
    }

    /**
     * GET /auth/user-status/{email}
     * Check user approval status (called after Google login)
     */
    @GetMapping("/user-status/{email}")
    public ResponseEntity<ApiResponse<UserStatusResponse>> getUserStatus(
            @PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Checking user status for: {}", email);
        try {
            UserStatusResponse response = userRegistrationService.getUserStatus(email, authorization);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("User status check failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /auth/check-approval/{email}
     * Quick check if user is approved (for routing)
     */
    @GetMapping("/check-approval/{email}")
    public ResponseEntity<ApiResponse<Boolean>> checkApproval(
            @PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Checking approval status for: {}", email);
        try {
            boolean isApproved = userRegistrationService.isUserApproved(email, authorization);
            return ResponseEntity.ok(ApiResponse.success(isApproved));
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Approval check failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    /**
     * GET /auth/user/{email}
     * Get user details
     */
    @GetMapping("/user/{email}")
    public ResponseEntity<ApiResponse<ClinicUserResponse>> getUser(
            @PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Getting user: {}", email);
        try {
            ClinicUserResponse response = userRegistrationService.getUser(email, authorization);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("Get user failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * PUT /auth/admin/approve/{email}
     * Approve a user registration (Admin only)
     */
    @PutMapping("/admin/approve/{email}")
    public ResponseEntity<ApiResponse<ClinicUserResponse>> approveUser(
            @PathVariable String email,
            @RequestParam(required = false) String clinicId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Approving user: {}", email);
        try {
            ClinicUserResponse response = userRegistrationService.approveUser(email, clinicId, authorization);
            return ResponseEntity.ok(ApiResponse.success("User approved successfully", response));
        } catch (IllegalArgumentException e) {
            log.warn("User approval failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * PUT /auth/admin/reject/{email}
     * Reject a user registration (Admin only)
     */
    @PutMapping("/admin/reject/{email}")
    public ResponseEntity<ApiResponse<ClinicUserResponse>> rejectUser(
            @PathVariable String email,
            @RequestParam(required = false) String rejectionReason,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Rejecting user: {}", email);
        try {
            ClinicUserResponse response = userRegistrationService.rejectUser(email, rejectionReason, authorization);
            return ResponseEntity.ok(ApiResponse.success("Registration rejected successfully.", response));
        } catch (IllegalArgumentException e) {
            log.warn("User rejection failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * PUT /auth/admin/suspend/{email}
     * Suspend a user account (Admin only)
     */
    @PutMapping("/admin/suspend/{email}")
    public ResponseEntity<ApiResponse<ClinicUserResponse>> suspendUser(
            @PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Suspending user: {}", email);
        try {
            ClinicUserResponse response = userRegistrationService.suspendUser(email, authorization);
            return ResponseEntity.ok(ApiResponse.success("User suspended successfully", response));
        } catch (IllegalArgumentException e) {
            log.warn("User suspension failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    @GetMapping("/admin/registrations")
    public ApiResponse<Page<ClinicUserResponse>> registrations(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String status, @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userRegistrationService.registrations(authorization, status, search, page, size));
    }

    @GetMapping("/admin/registrations/{email}/history")
    public ApiResponse<Page<RegistrationHistory>> history(@PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userRegistrationService.history(email, authorization, page, size));
    }

    @PutMapping("/admin/reactivate/{email}")
    public ApiResponse<ClinicUserResponse> reactivate(@PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success("User reactivated successfully.", userRegistrationService.reactivateUser(email, authorization));
    }

    @PutMapping("/admin/review/{email}")
    public ApiResponse<ClinicUserResponse> review(@PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success("Registration submitted for review.", userRegistrationService.submitForReview(email, authorization));
    }
}


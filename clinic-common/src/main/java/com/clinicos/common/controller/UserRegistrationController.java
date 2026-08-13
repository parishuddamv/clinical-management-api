package com.clinicos.common.controller;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.common.dto.ClinicUserResponse;
import com.clinicos.common.dto.RegistrationRequest;
import com.clinicos.common.dto.UserStatusResponse;
import com.clinicos.common.service.UserRegistrationService;
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
                    .body(ApiResponse.success("Registration successful! Please wait for approval.", response));
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    /**
     * GET /auth/user-status/{email}
     * Check user approval status (called after Google login)
     */
    @GetMapping("/user-status/{email}")
    public ResponseEntity<ApiResponse<UserStatusResponse>> getUserStatus(
            @PathVariable String email) {
        log.info("Checking user status for: {}", email);
        try {
            UserStatusResponse response = userRegistrationService.getUserStatus(email);
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
            @PathVariable String email) {
        log.info("Checking approval status for: {}", email);
        try {
            boolean isApproved = userRegistrationService.isUserApproved(email);
            return ResponseEntity.ok(ApiResponse.success(isApproved));
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
            @PathVariable String email) {
        log.info("Getting user: {}", email);
        try {
            ClinicUserResponse response = userRegistrationService.getUser(email);
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
            @RequestParam String clinicId,
            @RequestParam String approvedBy) {
        log.info("Approving user: {}", email);
        try {
            ClinicUserResponse response = userRegistrationService.approveUser(email, clinicId, approvedBy);
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
            @RequestParam String rejectionReason) {
        log.info("Rejecting user: {}", email);
        try {
            ClinicUserResponse response = userRegistrationService.rejectUser(email, rejectionReason);
            return ResponseEntity.ok(ApiResponse.success("User rejected successfully", response));
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
            @PathVariable String email) {
        log.info("Suspending user: {}", email);
        try {
            ClinicUserResponse response = userRegistrationService.suspendUser(email);
            return ResponseEntity.ok(ApiResponse.success("User suspended successfully", response));
        } catch (IllegalArgumentException e) {
            log.warn("User suspension failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}


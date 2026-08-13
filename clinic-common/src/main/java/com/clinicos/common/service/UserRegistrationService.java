package com.clinicos.common.service;

import com.clinicos.common.dto.ClinicUserResponse;
import com.clinicos.common.dto.RegistrationRequest;
import com.clinicos.common.dto.UserStatusResponse;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.repository.ClinicUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for user registration and approval workflow
 */
@Slf4j
@Service
public class UserRegistrationService {

    private final ObjectProvider<ClinicUserRepository> clinicUserRepositoryProvider;

    public UserRegistrationService(ObjectProvider<ClinicUserRepository> clinicUserRepositoryProvider) {
        this.clinicUserRepositoryProvider = clinicUserRepositoryProvider;
    }

    private ClinicUserRepository getRepository() {
        ClinicUserRepository repo = clinicUserRepositoryProvider.getIfAvailable();
        if (repo == null) {
            throw new RuntimeException("ClinicUserRepository not available");
        }
        return repo;
    }

    /**
     * Register a new user
     */
    @Transactional
    public ClinicUserResponse registerUser(RegistrationRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if user already exists
        if (getRepository().existsByEmail(request.getEmail())) {
            log.warn("User already exists: {}", request.getEmail());
            throw new IllegalArgumentException("User already registered with this email: " + request.getEmail());
        }

        // Create new clinic user
        ClinicUser clinicUser = ClinicUser.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(request.getRole())
                .phone(request.getPhone())
                .clinicName(request.getClinicName())
                .clinicAddress(request.getClinicAddress())
                .clinicPhone(request.getClinicPhone())
                .status(ClinicUser.UserStatus.NEW)
                .isActive(true)
                .isSuperAdmin(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ClinicUser savedUser = getRepository().save(clinicUser);
        log.info("User registered successfully: {}", savedUser.getEmail());

        return ClinicUserResponse.fromEntity(savedUser);
    }

    /**
     * Get user status (for checking approval after login)
     */
    public UserStatusResponse getUserStatus(String email) {
        log.info("Checking user status for: {}", email);

        Optional<ClinicUser> user = getRepository().findByEmail(email);

        if (user.isEmpty()) {
            log.warn("User not found: {}", email);
            throw new IllegalArgumentException("User not found: " + email);
        }

        return UserStatusResponse.fromClinicUser(user.get());
    }

    /**
     * Check if user is approved
     */
    public boolean isUserApproved(String email) {
        Optional<ClinicUser> user = getRepository().findByEmail(email);
        return user.map(u -> u.getStatus() == ClinicUser.UserStatus.APPROVED && u.getIsActive())
                .orElse(false);
    }

    /**
     * Approve a user registration (Admin only)
     */
    @Transactional
    public ClinicUserResponse approveUser(String email, String clinicId, String approvedBy) {
        log.info("Approving user: {} for clinic: {}", email, clinicId);

        Optional<ClinicUser> user = getRepository().findByEmail(email);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + email);
        }

        ClinicUser clinicUser = user.get();
        clinicUser.setStatus(ClinicUser.UserStatus.APPROVED);
        clinicUser.setClinicId(clinicId);
        clinicUser.setApprovedAt(LocalDateTime.now());
        clinicUser.setApprovedBy(approvedBy);
        clinicUser.setUpdatedAt(LocalDateTime.now());

        ClinicUser savedUser = getRepository().save(clinicUser);
        log.info("User approved: {}", email);

        return ClinicUserResponse.fromEntity(savedUser);
    }

    /**
     * Reject a user registration (Admin only)
     */
    @Transactional
    public ClinicUserResponse rejectUser(String email, String rejectionReason) {
        log.info("Rejecting user: {}", email);

        Optional<ClinicUser> user = getRepository().findByEmail(email);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + email);
        }

        ClinicUser clinicUser = user.get();
        clinicUser.setStatus(ClinicUser.UserStatus.REJECTED);
        clinicUser.setRejectionReason(rejectionReason);
        clinicUser.setUpdatedAt(LocalDateTime.now());

        ClinicUser savedUser = getRepository().save(clinicUser);
        log.info("User rejected: {}", email);

        return ClinicUserResponse.fromEntity(savedUser);
    }

    /**
     * Suspend a user account (Admin only)
     */
    @Transactional
    public ClinicUserResponse suspendUser(String email) {
        log.info("Suspending user: {}", email);

        Optional<ClinicUser> user = getRepository().findByEmail(email);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + email);
        }

        ClinicUser clinicUser = user.get();
        clinicUser.setStatus(ClinicUser.UserStatus.SUSPENDED);
        clinicUser.setIsActive(false);
        clinicUser.setUpdatedAt(LocalDateTime.now());

        ClinicUser savedUser = getRepository().save(clinicUser);
        log.info("User suspended: {}", email);

        return ClinicUserResponse.fromEntity(savedUser);
    }

    /**
     * Get user by email
     */
    public ClinicUserResponse getUser(String email) {
        log.info("Getting user: {}", email);

        Optional<ClinicUser> user = getRepository().findByEmail(email);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + email);
        }

        return ClinicUserResponse.fromEntity(user.get());
    }
}


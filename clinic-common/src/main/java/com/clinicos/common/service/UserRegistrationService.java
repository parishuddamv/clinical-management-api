package com.clinicos.common.service;

import com.clinicos.common.dto.ClinicUserResponse;
import com.clinicos.common.dto.RegistrationRequest;
import com.clinicos.common.dto.UserStatusResponse;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.entity.RegistrationHistory;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.RegistrationHistoryRepository;
import com.clinicos.common.security.RegistrationAccess;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Service for user registration and approval workflow
 */
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final ObjectProvider<ClinicUserRepository> clinicUserRepositoryProvider;
    private final ObjectProvider<RegistrationHistoryRepository> historyProvider;
    private final RegistrationAccess access;
    private final Validator validator;
    private final ObjectMapper objectMapper;

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
        if (request == null) throw badRequest("Registration body is required.");
        request.setEmail(normalize(request.getEmail()));
        request.setFullName(trim(request.getFullName()));
        request.setClinicName(trim(request.getClinicName()));
        request.setPhone(trim(request.getPhone()));
        request.setRole(request.getRole() == null ? null : request.getRole().trim().toUpperCase(Locale.ROOT));
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw badRequest(violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted().collect(java.util.stream.Collectors.joining("; ")));
        }
        getRepository().findByEmailIgnoreCase(request.getEmail()).ifPresent(existing -> {
            throw conflict(switch (existing.getStatus()) {
                case NEW, PENDING -> "Registration already exists and is awaiting approval.";
                case APPROVED -> "An approved account already exists for this email. Please sign in.";
                case REJECTED -> "Registration was rejected. Contact Super Admin to reopen the existing request.";
                case SUSPENDED -> "Account is suspended. Contact Super Admin.";
            });
        });

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
                .isActive(false)
                .isSuperAdmin(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try {
            clinicUser.setRegistrationDetails(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize registration", e);
        }
        ClinicUser savedUser;
        try {
            savedUser = getRepository().saveAndFlush(clinicUser);
        } catch (DataIntegrityViolationException e) {
            throw conflict("Registration already exists or conflicts with an existing account.");
        }
        record(savedUser, null, "REGISTERED", "PUBLIC_REGISTRATION", null);

        return ClinicUserResponse.fromEntity(savedUser);
    }

    /**
     * Get user status (for checking approval after login)
     */
    @Transactional(readOnly = true)
    public UserStatusResponse getUserStatus(String email, String authorization) {
        access.requireSelfOrSuperAdmin(authorization, email);
        return UserStatusResponse.fromClinicUser(find(email));
    }

    /**
     * Check if user is approved
     */
    @Transactional(readOnly = true)
    public boolean isUserApproved(String email, String authorization) {
        access.requireSelfOrSuperAdmin(authorization, email);
        return getRepository().findByEmailIgnoreCase(normalize(email))
                .map(u -> u.getStatus() == ClinicUser.UserStatus.APPROVED && Boolean.TRUE.equals(u.getIsActive()))
                .orElse(false);
    }

    /**
     * Approve a user registration (Super Admin only)
     */
    @Transactional
    public ClinicUserResponse approveUser(String email, String clinicId, String authorization) {
        String actor = access.requireSuperAdmin(authorization).getEmail();
        ClinicUser clinicUser = lock(email);
        requireReview(clinicUser);
        if (clinicId != null && !clinicId.matches("[A-Za-z0-9_-]{1,50}")) {
            throw badRequest("clinicId must contain 1-50 letters, digits, underscores or hyphens.");
        }
        ClinicUser.UserStatus previous = clinicUser.getStatus();
        clinicUser.setStatus(ClinicUser.UserStatus.APPROVED);
        clinicUser.setIsActive(true);
        clinicUser.setClinicId(clinicId != null ? clinicId :
                (clinicUser.getClinicId() != null && !clinicUser.getClinicId().isBlank()
                        ? clinicUser.getClinicId() : "CLINIC_" + UUID.randomUUID()));
        clinicUser.setApprovedAt(LocalDateTime.now());
        clinicUser.setApprovedBy(actor);
        clinicUser.setRejectionReason(null);
        return saveTransition(clinicUser, previous, "APPROVED", actor, null);
    }

    /**
     * Reject a user registration (Super Admin only)
     */
    @Transactional
    public ClinicUserResponse rejectUser(String email, String rejectionReason, String authorization) {
        String actor = access.requireSuperAdmin(authorization).getEmail();
        if (rejectionReason == null || rejectionReason.isBlank() || rejectionReason.length() > 2000) {
            throw badRequest("A rejection reason of 1-2000 characters is required.");
        }
        ClinicUser clinicUser = lock(email);
        requireReview(clinicUser);
        ClinicUser.UserStatus previous = clinicUser.getStatus();
        clinicUser.setStatus(ClinicUser.UserStatus.REJECTED);
        clinicUser.setIsActive(false);
        clinicUser.setRejectionReason(rejectionReason.trim());
        return saveTransition(clinicUser, previous, "REJECTED", actor, rejectionReason.trim());
    }

    /**
     * Suspend a user account (Super Admin only)
     */
    @Transactional
    public ClinicUserResponse suspendUser(String email, String authorization) {
        String actor = access.requireSuperAdmin(authorization).getEmail();
        ClinicUser clinicUser = lock(email);
        requireStatus(clinicUser, ClinicUser.UserStatus.APPROVED);
        clinicUser.setStatus(ClinicUser.UserStatus.SUSPENDED);
        clinicUser.setIsActive(false);
        return saveTransition(clinicUser, ClinicUser.UserStatus.APPROVED, "SUSPENDED", actor, null);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public ClinicUserResponse getUser(String email, String authorization) {
        access.requireSelfOrSuperAdmin(authorization, email);
        return ClinicUserResponse.fromEntity(find(email));
    }

    @Transactional
    public ClinicUserResponse reactivateUser(String email, String authorization) {
        String actor = access.requireSuperAdmin(authorization).getEmail();
        ClinicUser user = lock(email);
        requireStatus(user, ClinicUser.UserStatus.SUSPENDED);
        user.setStatus(ClinicUser.UserStatus.APPROVED);
        user.setIsActive(true);
        return saveTransition(user, ClinicUser.UserStatus.SUSPENDED, "REACTIVATED", actor, null);
    }

    @Transactional
    public ClinicUserResponse submitForReview(String email, String authorization) {
        String actor = access.requireSuperAdmin(authorization).getEmail();
        ClinicUser user = lock(email);
        if (user.getStatus() != ClinicUser.UserStatus.NEW && user.getStatus() != ClinicUser.UserStatus.REJECTED) {
            throw conflict("Only new or rejected registrations can be submitted for review.");
        }
        ClinicUser.UserStatus previous = user.getStatus();
        user.setStatus(ClinicUser.UserStatus.PENDING);
        user.setIsActive(false);
        user.setRejectionReason(null);
        return saveTransition(user, previous, "SUBMITTED_FOR_REVIEW", actor, null);
    }

    @Transactional(readOnly = true)
    public Page<ClinicUserResponse> registrations(String authorization, String status, String search, int page, int size) {
        access.requireSuperAdmin(authorization);
        validatePage(page, size);
        Specification<ClinicUser> filter = (root, query, cb) -> cb.conjunction();
        if (status != null && !status.isBlank()) {
            ClinicUser.UserStatus value;
            try {
                value = ClinicUser.UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw badRequest("Unknown registration status.");
            }
            filter = filter.and((root, query, cb) -> cb.equal(root.get("status"), value));
        }
        if (search != null && !search.isBlank()) {
            if (search.length() > 200) throw badRequest("Search must not exceed 200 characters.");
            String term = "%" + normalize(search).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            filter = filter.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), term, '\\'),
                    cb.like(cb.lower(root.get("email")), term, '\\'),
                    cb.like(cb.lower(root.get("clinicName")), term, '\\'),
                    cb.like(cb.lower(root.get("clinicId")), term, '\\')));
        }
        return getRepository().findAll(filter, PageRequest.of(page, size, Sort.by("createdAt", "id").descending()))
                .map(ClinicUserResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<RegistrationHistory> history(String email, String authorization, int page, int size) {
        access.requireSuperAdmin(authorization);
        validatePage(page, size);
        return historyProvider.getObject().findByUserIdOrderByPerformedAtDescIdDesc(find(email).getId(), PageRequest.of(page, size));
    }

    private ClinicUserResponse saveTransition(ClinicUser user, ClinicUser.UserStatus previous, String action, String actor, String reason) {
        user.setUpdatedAt(LocalDateTime.now());
        getRepository().save(user);
        record(user, previous, action, actor, reason);
        return ClinicUserResponse.fromEntity(user);
    }

    private void record(ClinicUser user, ClinicUser.UserStatus previous, String action, String actor, String reason) {
        historyProvider.getObject().save(RegistrationHistory.builder().userId(user.getId())
                .previousStatus(previous).newStatus(user.getStatus()).action(action).performedBy(actor)
                .performedAt(LocalDateTime.now()).rejectionReason(reason).build());
    }

    private ClinicUser find(String email) {
        return getRepository().findByEmailIgnoreCase(normalize(email)).orElseThrow(() ->
                new RegistrationException(HttpStatus.NOT_FOUND, "Registration not found."));
    }

    private ClinicUser lock(String email) {
        return getRepository().lockByEmail(normalize(email)).orElseThrow(() ->
                new RegistrationException(HttpStatus.NOT_FOUND, "Registration not found."));
    }

    private void requireReview(ClinicUser user) {
        if (user.getStatus() != ClinicUser.UserStatus.NEW && user.getStatus() != ClinicUser.UserStatus.PENDING) {
            throw conflict("Registration is not awaiting review.");
        }
    }

    private void requireStatus(ClinicUser user, ClinicUser.UserStatus expected) {
        if (user.getStatus() != expected) throw conflict("Operation requires status " + expected + ".");
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw badRequest("page must be non-negative and size must be 1-100.");
    }

    private static String trim(String text) { return text == null ? null : text.trim(); }
    private static String normalize(String text) { return text == null ? null : text.trim().toLowerCase(Locale.ROOT); }
    private static RegistrationException badRequest(String message) {
        return new RegistrationException(HttpStatus.BAD_REQUEST, message);
    }
    private static RegistrationException conflict(String message) {
        return new RegistrationException(HttpStatus.CONFLICT, message);
    }
}


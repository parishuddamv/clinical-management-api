package com.clinicos.billing.subscription.service;

import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.ClinicAccessDeniedException;
import com.clinicos.common.repository.ClinicUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final ClinicUserRepository clinicUserRepository;
    private final JdbcTemplate jdbcTemplate;

    public ActorContext resolveActor(Authentication authentication) {
        String email = String.valueOf(authentication.getPrincipal());
        String clinicId = (String) authentication.getDetails();

        ClinicUser user = clinicUserRepository.findByEmailAndClinicId(email, clinicId)
                .or(() -> clinicUserRepository.findByEmail(email))
                .orElseThrow(() -> new ClinicAccessDeniedException("Authenticated user record not found"));

        return ActorContext.builder()
                .email(user.getEmail())
                .clinicId(clinicId)
                .role(normalizeRole(user.getRole()))
                .superAdmin(Boolean.TRUE.equals(user.getIsSuperAdmin()))
                .build();
    }

    public void requirePermission(String clinicId, String role, String permission) {
        String normalizedRole = normalizeRole(role);
        if (isOwnerLike(normalizedRole)) {
            return;
        }

        Set<String> permissions = permissionsForRole(clinicId, normalizedRole);
        if (!permissions.contains(permission)) {
            throw new ClinicAccessDeniedException("Permission denied: " + permission + " is required");
        }
    }

    public void requirePlatformAdmin(ActorContext actor) {
        if (!actor.isSuperAdmin()) {
            throw new ClinicAccessDeniedException("Only platform administrators can perform this action");
        }
    }

    public Set<String> permissionsForRole(String clinicId, String role) {
        try {
            List<String> rows = jdbcTemplate.queryForList(
                    "SELECT permission FROM role_permissions WHERE (clinic_id = ? OR clinic_id = 'DEFAULT') AND role = ?",
                    String.class,
                    clinicId,
                    normalizeRole(role)
            );
            return new HashSet<>(rows);
        } catch (DataAccessException ex) {
            // Backward-compatible fallback when staff tables are not available in an environment.
            log.warn("Role permission lookup failed for clinic {} role {}: {}", clinicId, role, ex.getMessage());
            return Set.of();
        }
    }

    private boolean isOwnerLike(String role) {
        String normalized = normalizeRole(role);
        return "OWNER".equals(normalized) || "ADMIN".equals(normalized);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }
}


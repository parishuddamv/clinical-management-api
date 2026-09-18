package com.clinicos.common.dto;

import com.clinicos.common.entity.ClinicUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for ClinicUser with all details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicUserResponse {

    private Long id;

    private String email;

    private String fullName;

    private String role;

    private String phone;

    private String clinicId;

    private String clinicName;

    private String clinicAddress;

    private String clinicPhone;

    private String status; // NEW, PENDING, APPROVED, REJECTED, SUSPENDED

    private Boolean isActive;

    private Boolean isSuperAdmin;

    private LocalDateTime approvedAt;

    private String approvedBy;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastLogin;

    private String registrationDetails;

    /**
     * Convert entity to response DTO
     */
    public static ClinicUserResponse fromEntity(ClinicUser user) {
        return ClinicUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .phone(user.getPhone())
                .clinicId(user.getClinicId())
                .clinicName(user.getClinicName())
                .clinicAddress(user.getClinicAddress())
                .clinicPhone(user.getClinicPhone())
                .status(user.getStatus().name())
                .isActive(user.getIsActive())
                .isSuperAdmin(user.getIsSuperAdmin())
                .approvedAt(user.getApprovedAt())
                .approvedBy(user.getApprovedBy())
                .rejectionReason(user.getRejectionReason())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .registrationDetails(user.getRegistrationDetails())
                .build();
    }
}


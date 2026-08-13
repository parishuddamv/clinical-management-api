package com.clinicos.common.dto;

import com.clinicos.common.entity.ClinicUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO to check user approval status after login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatusResponse {

    private String email;
    private String status; // NEW, PENDING, APPROVED, REJECTED, SUSPENDED
    private Boolean isApproved;
    private Boolean needsDemoBooking;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private String rejectionReason;

    public static UserStatusResponse fromClinicUser(ClinicUser user) {
        boolean isApproved = user.getStatus() == ClinicUser.UserStatus.APPROVED;
        boolean needsDemo = !isApproved && user.getStatus() != ClinicUser.UserStatus.REJECTED;

        return UserStatusResponse.builder()
                .email(user.getEmail())
                .status(user.getStatus().name())
                .isApproved(isApproved)
                .needsDemoBooking(needsDemo)
                .message(isApproved ? "User approved" : "Booking demo required")
                .createdAt(user.getCreatedAt())
                .approvedAt(user.getApprovedAt())
                .rejectionReason(user.getRejectionReason())
                .build();
    }
}


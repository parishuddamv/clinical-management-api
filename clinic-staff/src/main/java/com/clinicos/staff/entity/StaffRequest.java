package com.clinicos.staff.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "staff_requests",
    indexes = {
        @Index(name = "idx_staff_requests_clinic", columnList = "clinic_id"),
        @Index(name = "idx_staff_requests_status", columnList = "clinic_id, status"),
        @Index(name = "idx_staff_requests_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false, length = 50)
    private String clinicId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_role", nullable = false, length = 50)
    private StaffMember.StaffRole requestedRole;

    @Column(name = "requested_by", nullable = false, length = 255)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private StaffRequestStatus status = StaffRequestStatus.PENDING_APPROVAL;

    @Column(name = "approval_required", nullable = false)
    @Builder.Default
    private boolean approvalRequired = false;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "invitation_sent_at")
    private LocalDateTime invitationSentAt;

    @Column(name = "invitation_expires_at")
    private LocalDateTime invitationExpiresAt;

    @Column(name = "invitation_accepted_at")
    private LocalDateTime invitationAcceptedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum StaffRequestStatus {
        PENDING_APPROVAL,
        APPROVED,
        REJECTED,
        EXPIRED,
        CANCELLED
    }
}
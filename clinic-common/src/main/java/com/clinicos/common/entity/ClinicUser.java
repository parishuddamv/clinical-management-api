package com.clinicos.common.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ClinicUser Entity - Represents clinic user registration and approval status
 * Tracks new clinic registrations waiting for admin approval
 */
@Entity
@Table(name = "clinic_users", indexes = {
    @Index(name = "idx_clinic_user_email", columnList = "email", unique = true),
    @Index(name = "idx_clinic_user_status", columnList = "status"),
    @Index(name = "idx_clinic_user_clinic_id", columnList = "clinic_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 50)
    private String role; // ADMIN, DOCTOR, RECEPTIONIST, etc.

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "clinic_id", length = 50)
    private String clinicId;

    @Column(name = "clinic_name", length = 200)
    private String clinicName;

    @Column(name = "clinic_address", columnDefinition = "TEXT")
    private String clinicAddress;

    @Column(name = "clinic_phone", length = 20)
    private String clinicPhone;

    /**
     * User Status:
     * - NEW: User just signed up, not yet approved
     * - PENDING: Admin reviewing the registration
     * - APPROVED: User approved and can use the system
     * - REJECTED: Registration was rejected
     * - SUSPENDED: User account suspended
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.NEW;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_super_admin")
    @Builder.Default
    private Boolean isSuperAdmin = false;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /** Original submitted fields, including optional demo and practice information (JSON text). */
    @Column(name = "registration_details", columnDefinition = "TEXT")
    private String registrationDetails;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum UserStatus {
        NEW,        // Just registered
        PENDING,    // Awaiting admin approval
        APPROVED,   // Approved and active
        REJECTED,   // Registration rejected
        SUSPENDED   // Account suspended
    }
}


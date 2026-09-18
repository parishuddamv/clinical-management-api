package com.clinicos.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RegistrationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30, updatable = false)
    private ClinicUser.UserStatus previousStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30, updatable = false)
    private ClinicUser.UserStatus newStatus;
    @Column(nullable = false, length = 40, updatable = false)
    private String action;
    @Column(name = "performed_by", nullable = false, length = 100, updatable = false)
    private String performedBy;
    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;
    @Column(name = "rejection_reason", columnDefinition = "TEXT", updatable = false)
    private String rejectionReason;
}

package com.clinicos.notification.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Notification entity for tracking sent notifications
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_clinic_patient", columnList = "clinic_id, patient_id"),
    @Index(name = "idx_clinic_status", columnList = "clinic_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String recipient;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    public enum NotificationType {
        EMAIL, SMS, PUSH
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, DELIVERED
    }
}


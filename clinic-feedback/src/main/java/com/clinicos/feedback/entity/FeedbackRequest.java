package com.clinicos.feedback.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "feedback_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackRequest extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "visit_id")
    private Long visitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private RequestType requestType;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "feedback_token", unique = true, length = 100)
    private String feedbackToken;

    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id")
    private PatientFeedback feedback;

    @Column(name = "reminder_count")
    @Builder.Default
    private Integer reminderCount = 0;

    @Column(name = "last_reminder_at")
    private LocalDateTime lastReminderAt;

    @PrePersist
    protected void onCreate() {
        if (feedbackToken == null) {
            feedbackToken = UUID.randomUUID().toString();
        }
        if (sentAt == null) sentAt = LocalDateTime.now();
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusDays(7);
    }

    public void markCompleted(PatientFeedback feedback) {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
        this.feedback = feedback;
    }

    public enum RequestType {
        SMS,
        EMAIL,
        WHATSAPP,
        APP_NOTIFICATION
    }
}


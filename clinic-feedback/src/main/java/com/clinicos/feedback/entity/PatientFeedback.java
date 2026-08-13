package com.clinicos.feedback.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientFeedback extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "visit_id")
    private Long visitId;

    @Column(name = "doctor_id")
    private Long doctorId;

    // Ratings
    @Column(name = "overall_rating", nullable = false)
    private Integer overallRating;

    @Column(name = "doctor_rating")
    private Integer doctorRating;

    @Column(name = "staff_rating")
    private Integer staffRating;

    @Column(name = "facility_rating")
    private Integer facilityRating;

    @Column(name = "wait_time_rating")
    private Integer waitTimeRating;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "would_recommend")
    private Boolean wouldRecommend;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", length = 30)
    @Builder.Default
    private FeedbackType feedbackType = FeedbackType.GENERAL;

    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private FeedbackStatus status = FeedbackStatus.SUBMITTED;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "is_anonymous")
    @Builder.Default
    private Boolean isAnonymous = false;

    @Column(name = "clinic_response", columnDefinition = "TEXT")
    private String clinicResponse;

    @Column(name = "responded_by", length = 100)
    private String respondedBy;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_source", length = 30)
    @Builder.Default
    private FeedbackSource feedbackSource = FeedbackSource.APP;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onSubmit() {
        if (submittedAt == null) submittedAt = LocalDateTime.now();
    }

    public enum FeedbackType {
        GENERAL,
        COMPLAINT,
        SUGGESTION,
        APPRECIATION
    }

    public enum FeedbackStatus {
        SUBMITTED,
        REVIEWED,
        RESOLVED,
        ARCHIVED
    }

    public enum FeedbackSource {
        APP,
        SMS,
        EMAIL,
        IN_PERSON,
        WHATSAPP,
        WEBSITE
    }
}


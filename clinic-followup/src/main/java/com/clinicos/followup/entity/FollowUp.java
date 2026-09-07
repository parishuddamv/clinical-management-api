package com.clinicos.followup.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * FollowUp entity for tracking patient follow-up tasks and notes
 */
@Entity
@Table(name = "followups", indexes = {
    @Index(name = "idx_patients", columnList = "clinic_id, patient_id"),
    @Index(name = "idx_clinic_duedate", columnList = "clinic_id, due_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUp extends BaseEntity {

    @Column(nullable = false)
    private Long patientId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FollowUpStatus status = FollowUpStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String clinicalNotes;

    public enum FollowUpStatus {
        PENDING, COMPLETED, CANCELLED, OVERDUE
    }
}


package com.clinicos.emr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A dental treatment plan for a patient: per-tooth procedures with costs, sittings and lab work.
 * The items are stored as JSON in {@code data}; totals and the next lab due date are derived on save.
 */
@Entity
@Table(name = "dental_treatment_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DentalTreatmentPlan {

    public enum Status { DRAFT, PROPOSED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false, length = 50)
    private String clinicId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "patient_name", length = 200)
    private String patientName;

    @Column(length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "billed_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal billedAmount = BigDecimal.ZERO;

    @Column(name = "next_lab_due")
    private LocalDate nextLabDue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String data;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

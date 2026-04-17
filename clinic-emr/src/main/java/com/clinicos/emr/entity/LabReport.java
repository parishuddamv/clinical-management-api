package com.clinicos.emr.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.Map;

/**
 * Lab Report entity for managing laboratory test results.
 */
@Entity
@Table(name = "lab_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private PatientVisit visit;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "report_number", length = 50)
    private String reportNumber;

    @Column(name = "test_category", length = 100)
    private String testCategory;

    @Column(name = "test_name", nullable = false, length = 200)
    private String testName;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Column(name = "result_date")
    private LocalDate resultDate;

    @Column(name = "lab_name", length = 200)
    private String labName;

    @Column(name = "ordering_doctor", length = 200)
    private String orderingDoctor;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_values", columnDefinition = "jsonb")
    private Map<String, Object> resultValues;

    @Enumerated(EnumType.STRING)
    @Column(name = "interpretation", length = 50)
    private Interpretation interpretation;

    @Column(name = "reference_range", columnDefinition = "TEXT")
    private String referenceRange;

    @Column(name = "is_abnormal")
    @Builder.Default
    private Boolean isAbnormal = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LabReportStatus status = LabReportStatus.PENDING;

    public enum LabReportStatus {
        PENDING,
        SAMPLE_COLLECTED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    public enum Interpretation {
        NORMAL,
        ABNORMAL,
        CRITICAL,
        BORDERLINE,
        INCONCLUSIVE
    }
}


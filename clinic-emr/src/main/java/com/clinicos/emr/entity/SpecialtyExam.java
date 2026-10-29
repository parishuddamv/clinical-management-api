package com.clinicos.emr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A specialty chart for a patient (dental chart, eye exam or skin lesions), stored as JSON. */
@Entity
@Table(name = "specialty_exams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyExam {

    public enum ExamType { DENTAL, EYE, SKIN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false, length = 50)
    private String clinicId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "visit_id")
    private Long visitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 20)
    private ExamType examType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String data;

    @Column(name = "recorded_by", nullable = false, length = 100)
    private String recordedBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

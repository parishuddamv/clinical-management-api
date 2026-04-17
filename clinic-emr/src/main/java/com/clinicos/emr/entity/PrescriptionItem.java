package com.clinicos.emr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Prescription Item entity - individual medications in a prescription.
 */
@Entity
@Table(name = "prescription_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id")
    private DrugMaster drug;

    @Column(name = "drug_name", nullable = false, length = 200)
    private String drugName;

    @Column(name = "generic_name", length = 200)
    private String genericName;

    @Column(name = "strength", length = 100)
    private String strength;

    @Column(name = "form", length = 50)
    private String form;

    @Column(name = "dosage", nullable = false, length = 100)
    private String dosage;

    @Column(name = "frequency", nullable = false, length = 100)
    private String frequency;

    @Column(name = "duration", nullable = false, length = 100)
    private String duration;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "route", length = 50)
    private String route;

    @Column(name = "timing", length = 100)
    private String timing;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_after_food", length = 20)
    private FoodTiming beforeAfterFood;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(name = "sequence_order")
    @Builder.Default
    private Integer sequenceOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum FoodTiming {
        BEFORE_FOOD,
        AFTER_FOOD,
        WITH_FOOD,
        EMPTY_STOMACH,
        ANY_TIME
    }
}


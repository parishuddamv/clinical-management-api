package com.clinicos.emr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A doctor's saved medicine with its usual dose ("dose template"). */
@Entity
@Table(name = "doctor_favourite_drugs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavouriteDrug {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false, length = 50)
    private String clinicId;

    @Column(name = "doctor_user", nullable = false, length = 100)
    private String doctorUser;

    @Column(name = "drug_name", nullable = false, length = 200)
    private String drugName;

    @Column(name = "generic_name", length = 200)
    private String genericName;

    @Column(length = 50)
    private String strength;

    @Column(length = 50)
    private String form;

    @Column(length = 100)
    private String dosage;

    @Column(length = 50)
    private String frequency;

    @Column(length = 50)
    private String duration;

    @Column(length = 20)
    private String route;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_after_food", length = 20)
    private PrescriptionItem.FoodTiming beforeAfterFood;

    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    @Column(name = "use_count", nullable = false)
    @Builder.Default
    private Integer useCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

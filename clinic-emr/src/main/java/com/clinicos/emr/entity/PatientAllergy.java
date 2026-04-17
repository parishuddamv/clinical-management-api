package com.clinicos.emr.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Patient Allergy entity for tracking patient allergies.
 */
@Entity
@Table(name = "patient_allergies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientAllergy extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "allergen_type", nullable = false, length = 50)
    private AllergenType allergenType;

    @Column(name = "allergen_name", nullable = false, length = 200)
    private String allergenName;

    @Column(name = "reaction_type", length = 100)
    private String reactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private AllergySeverity severity;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_by", length = 100)
    private String recordedBy;

    public enum AllergenType {
        DRUG,            // Drug/Medication allergy
        FOOD,            // Food allergy
        ENVIRONMENTAL,   // Environmental (pollen, dust, etc.)
        INSECT,          // Insect stings/bites
        LATEX,           // Latex allergy
        CHEMICAL,        // Chemical sensitivity
        OTHER            // Other allergens
    }

    public enum AllergySeverity {
        MILD,
        MODERATE,
        SEVERE,
        LIFE_THREATENING
    }
}

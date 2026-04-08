package com.clinicos.patient.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Patient entity representing a patient in a clinic.
 * Multi-tenant: clinic_id ensures clinic isolation.
 */
@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_clinic_phone", columnList = "clinic_id, phone", unique = false),
    @Index(name = "idx_clinic_created", columnList = "clinic_id, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Patient extends BaseEntity {

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = false)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}


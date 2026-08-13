package com.clinicos.staff.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "staff_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffMember extends BaseEntity {

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private StaffRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private StaffStatus status = StaffStatus.ACTIVE;

    // Professional Details (for doctors)
    @Column(name = "specialization", length = 200)
    private String specialization;

    @Column(name = "qualification", length = 500)
    private String qualification;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    // Address
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public enum StaffRole {
        ADMIN,
        DOCTOR,
        RECEPTIONIST,
        NURSE,
        LAB_TECHNICIAN,
        PHARMACIST,
        ACCOUNTANT
    }

    public enum StaffStatus {
        ACTIVE,
        INACTIVE,
        ON_LEAVE,
        TERMINATED
    }
}


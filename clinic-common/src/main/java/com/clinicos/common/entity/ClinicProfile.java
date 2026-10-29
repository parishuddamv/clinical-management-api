package com.clinicos.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** What a clinic prints on prescriptions and invoices, how it takes UPI payments, and its booking page settings. */
@Entity
@Table(name = "clinic_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicProfile {

    @Id
    @Column(name = "clinic_id", length = 50)
    private String clinicId;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(length = 200)
    private String tagline;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 15)
    private String gstin;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "working_hours", length = 200)
    private String workingHours;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "upi_payee_name", length = 100)
    private String upiPayeeName;

    @Column(name = "prescription_language", nullable = false, length = 10)
    @Builder.Default
    private String prescriptionLanguage = "en";

    @Column(name = "prescription_footer", length = 300)
    private String prescriptionFooter;

    @Column(name = "booking_enabled", nullable = false)
    @Builder.Default
    private Boolean bookingEnabled = false;

    @Column(name = "booking_slug", length = 50)
    private String bookingSlug;

    @Column(name = "booking_intro", length = 500)
    private String bookingIntro;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}

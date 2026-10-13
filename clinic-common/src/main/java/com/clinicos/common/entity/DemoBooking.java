package com.clinicos.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demo_bookings", indexes = {
    @Index(name = "idx_demo_booking_email", columnList = "email"),
    @Index(name = "idx_demo_booking_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DemoBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 50)
    private String role;

    @Column(length = 20)
    private String phone;

    @Column(name = "clinic_name", length = 200)
    private String clinicName;

    @Column(name = "clinic_address", columnDefinition = "TEXT")
    private String clinicAddress;

    @Column(name = "clinic_phone", length = 20)
    private String clinicPhone;

    @Column(name = "demo_date")
    private LocalDate demoDate;

    @Column(name = "demo_time", length = 10)
    private String demoTime;

    @Column(name = "demo_timezone", length = 50)
    private String demoTimezone;

    @Column(name = "preferred_language", length = 20)
    @Builder.Default
    private String preferredLanguage = "en";

    @Column(name = "number_of_users")
    private Integer numberOfUsers;

    @Column(length = 200)
    private String specialization;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DemoBookingStatus status = DemoBookingStatus.PENDING;

    @Column(name = "demo_link", length = 500)
    private String demoLink;

    @Column(name = "scheduled_by", length = 100)
    private String scheduledBy;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "feedback_rating")
    private Integer feedbackRating;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void confirm(String demoLink, String scheduledBy) {
        this.status = DemoBookingStatus.CONFIRMED;
        this.demoLink = demoLink;
        this.scheduledBy = scheduledBy;
        this.scheduledAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = DemoBookingStatus.CANCELLED;
    }

    public enum DemoBookingStatus {
        PENDING,
        CONFIRMED,
        COMPLETED,
        CANCELLED
    }
}

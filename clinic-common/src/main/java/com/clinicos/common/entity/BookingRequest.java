package com.clinicos.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** An appointment request made on a clinic's public booking page. */
@Entity
@Table(name = "booking_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    public enum Status { NEW, CONFIRMED, DECLINED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false, length = 50)
    private String clinicId;

    @Column(nullable = false, length = 12)
    private String reference;

    @Column(name = "patient_name", nullable = false, length = 100)
    private String patientName;

    @Column(nullable = false, length = 20)
    private String phone;

    private Integer age;

    @Column(length = 10)
    private String gender;

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @Column(name = "preferred_time", nullable = false, length = 10)
    private String preferredTime;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "doctor_name", length = 120)
    private String doctorName;

    @Column(length = 300)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.NEW;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "handled_by", length = 100)
    private String handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "requester_ip", length = 64)
    private String requesterIp;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

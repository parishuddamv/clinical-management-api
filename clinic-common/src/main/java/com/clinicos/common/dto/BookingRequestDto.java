package com.clinicos.common.dto;

import com.clinicos.common.entity.BookingRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A booking request: sent by the public page, read and updated by the clinic. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class BookingRequestDto {
    private Long id;
    private String reference;
    private String patientName;
    private String phone;
    private Integer age;
    private String gender;
    private LocalDate preferredDate;
    /** MORNING, AFTERNOON, EVENING, or a time such as 10:30 */
    private String preferredTime;
    private Long doctorId;
    private String doctorName;
    private String reason;
    private String status;
    private Long appointmentId;
    private LocalDateTime createdAt;
    /** Public form only: the patient agreed to be contacted about the appointment. */
    private Boolean consent;
    /** Public form only: a hidden field that people leave empty and spam bots fill in. */
    private String website;

    public static BookingRequestDto from(BookingRequest b) {
        return BookingRequestDto.builder().id(b.getId()).reference(b.getReference()).patientName(b.getPatientName())
                .phone(b.getPhone()).age(b.getAge()).gender(b.getGender()).preferredDate(b.getPreferredDate())
                .preferredTime(b.getPreferredTime()).doctorId(b.getDoctorId()).doctorName(b.getDoctorName())
                .reason(b.getReason()).status(b.getStatus().name()).appointmentId(b.getAppointmentId())
                .createdAt(b.getCreatedAt()).build();
    }
}

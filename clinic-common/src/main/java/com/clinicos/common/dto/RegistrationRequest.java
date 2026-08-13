package com.clinicos.common.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for user registration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Role is required")
    private String role; // ADMIN, DOCTOR, RECEPTIONIST, etc.

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9\\-\\+\\s()]+$", message = "Phone must be a valid phone number")
    private String phone;

    @NotBlank(message = "Clinic name is required")
    @Size(min = 2, max = 200, message = "Clinic name must be 2-200 characters")
    private String clinicName;

    private String clinicAddress;

    private String clinicPhone;

    private LocalDate demoDate;

    private String demoTime;

    private String demoTimezone;

    private String preferredLanguage;

    private Integer numberOfUsers;

    private String specialization;

    private String additionalNotes;
}


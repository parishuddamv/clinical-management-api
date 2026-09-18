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
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|DOCTOR|RECEPTIONIST|NURSE|LAB_TECHNICIAN|PHARMACIST|ACCOUNTANT|MANAGER|BILLING",
            message = "Select a supported clinic role; Super Admin cannot be requested")
    private String role; // ADMIN, DOCTOR, RECEPTIONIST, etc.

    @NotBlank(message = "Phone is required")
    @Size(min = 7, max = 20)
    @Pattern(regexp = "^(?=(?:[^0-9]*[0-9]){7,15}[^0-9]*$)\\+?[0-9\\- ()]+$", message = "Phone must contain 7-15 digits")
    private String phone;

    @NotBlank(message = "Clinic name is required")
    @Size(min = 2, max = 200, message = "Clinic name must be 2-200 characters")
    private String clinicName;

    private String clinicAddress;

    @Size(max = 20)
    private String clinicPhone;

    private LocalDate demoDate;

    private String demoTime;

    private String demoTimezone;

    private String preferredLanguage;

    private Integer numberOfUsers;

    private String specialization;

    private String additionalNotes;
}


package com.clinicos.patient.dto;

import com.clinicos.patient.entity.Patient;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating patient information
 * All fields are optional for partial updates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePatientRequest {

    @Size(min = 1, max = 100, message = "First name must be 1-100 characters")
    private String firstName;

    @Size(min = 1, max = 100, message = "Last name must be 1-100 characters")
    private String lastName;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10-digit Indian number")
    private String phone;

    @PastOrPresent(message = "Date of birth cannot be in future")
    private LocalDate dateOfBirth;

    private Patient.Gender gender;

    @Pattern(regexp = "^(O|A|B|AB)[+-]?$", message = "Invalid blood group")
    private String bloodGroup;

    @Size(max = 500, message = "Address must be 0-500 characters")
    private String address;

    @Size(max = 100, message = "Emergency contact name must be 0-100 characters")
    private String emergencyContactName;

    @Pattern(regexp = "^([6-9]\\d{9})?$", message = "Emergency contact phone must be valid 10-digit number")
    private String emergencyContactPhone;

    private Boolean isActive;
}


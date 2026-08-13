package com.clinicos.staff.dto;

import com.clinicos.staff.entity.StaffMember.StaffRole;
import com.clinicos.staff.entity.StaffMember.StaffStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMemberDTO {

    private Long id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String profileImageUrl;

    @NotNull(message = "Role is required")
    private StaffRole role;

    private StaffStatus status;

    // Doctor-specific fields
    private String specialization;
    private String qualification;
    private String licenseNumber;
    private Integer experienceYears;
    private BigDecimal consultationFee;

    // Address
    private String address;
    private String city;
    private String state;
    private String pincode;

    private LocalDate joinedDate;
    private String notes;

    // Computed fields
    private String fullName;
    private Set<String> permissions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


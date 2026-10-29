package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** What the booking page shows about a doctor. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicDoctorDto {
    private Long id;
    private String name;
    private String specialization;
}

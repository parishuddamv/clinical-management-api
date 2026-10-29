package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The little a booking page may show about a clinic. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicClinicDto {
    private String slug;
    private String displayName;
    private String tagline;
    private String address;
    private String phone;
    private String workingHours;
    private String bookingIntro;
    private String brandColor;
    private String bookingTheme;
    private String logo;
}

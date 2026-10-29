package com.clinicos.common.dto;

import com.clinicos.common.entity.ClinicProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Clinic profile as read and saved by clinic admins. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ClinicProfileDto {
    private String clinicId;
    private String displayName;
    private String tagline;
    private String address;
    private String phone;
    private String email;
    private String gstin;
    private String registrationNumber;
    private String workingHours;
    private String upiId;
    private String upiPayeeName;
    private String prescriptionLanguage;
    private String prescriptionFooter;
    private Boolean bookingEnabled;
    private String bookingSlug;
    private String bookingIntro;
    private String whatsappNumber;
    private String brandColor;
    private String bookingTheme;
    /** Logo as a data URL; null or empty to remove it. */
    private String logo;
    /** True until an admin has saved the profile once (values are defaults from registration). */
    private Boolean draft;

    public static ClinicProfileDto from(ClinicProfile p, boolean draft) {
        return ClinicProfileDto.builder()
                .clinicId(p.getClinicId()).displayName(p.getDisplayName()).tagline(p.getTagline())
                .address(p.getAddress()).phone(p.getPhone()).email(p.getEmail()).gstin(p.getGstin())
                .registrationNumber(p.getRegistrationNumber()).workingHours(p.getWorkingHours())
                .upiId(p.getUpiId()).upiPayeeName(p.getUpiPayeeName())
                .prescriptionLanguage(p.getPrescriptionLanguage()).prescriptionFooter(p.getPrescriptionFooter())
                .bookingEnabled(p.getBookingEnabled()).bookingSlug(p.getBookingSlug()).bookingIntro(p.getBookingIntro())
                .whatsappNumber(p.getWhatsappNumber())
                .brandColor(p.getBrandColor()).bookingTheme(p.getBookingTheme()).logo(p.getLogo()).draft(draft)
                .build();
    }
}

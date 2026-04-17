package com.clinicos.emr.dto;

import com.clinicos.emr.entity.PrescriptionItem.FoodTiming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrescriptionRequest {

    private Long visitId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    private String doctorName;

    private String doctorLicenseNo;

    private String doctorSpecialization;

    private LocalDate prescriptionDate;

    private LocalDate validUntil;

    private String diagnosisSummary;

    private String specialInstructions;

    @NotEmpty(message = "At least one medication is required")
    @Valid
    private List<PrescriptionItemRequest> items;

    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionItemRequest {
        private Long drugId;

        @NotBlank(message = "Drug name is required")
        private String drugName;

        private String genericName;

        private String strength;

        private String form;

        @NotBlank(message = "Dosage is required")
        private String dosage;

        @NotBlank(message = "Frequency is required")
        private String frequency;

        @NotBlank(message = "Duration is required")
        private String duration;

        private Integer quantity;

        private String route;

        private String timing;

        private FoodTiming beforeAfterFood;

        private String specialInstructions;

        private Integer sequenceOrder;
    }
}


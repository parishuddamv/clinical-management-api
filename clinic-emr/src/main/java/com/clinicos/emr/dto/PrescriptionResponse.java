package com.clinicos.emr.dto;

import com.clinicos.emr.entity.Prescription.DeliveryMethod;
import com.clinicos.emr.entity.Prescription.PrescriptionStatus;
import com.clinicos.emr.entity.PrescriptionItem.FoodTiming;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponse {
    private Long id;
    private String prescriptionNumber;
    private Long visitId;
    private Long patientId;
    private String doctorId;
    private String doctorName;
    private String doctorLicenseNo;
    private String doctorSpecialization;
    private LocalDate prescriptionDate;
    private LocalDate validUntil;
    private String diagnosisSummary;
    private String specialInstructions;
    private PrescriptionStatus status;
    private DeliveryMethod deliveryMethod;
    private LocalDateTime deliveredAt;
    private String deliveryReference;
    private String pdfFilePath;
    private String notes;
    private List<PrescriptionItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionItemResponse {
        private Long id;
        private Long drugId;
        private String drugName;
        private String genericName;
        private String strength;
        private String form;
        private String dosage;
        private String frequency;
        private String duration;
        private Integer quantity;
        private String route;
        private String timing;
        private FoodTiming beforeAfterFood;
        private String specialInstructions;
        private Integer sequenceOrder;
    }
}


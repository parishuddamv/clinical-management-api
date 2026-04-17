package com.clinicos.emr.dto;

import com.clinicos.emr.entity.Prescription.DeliveryMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverPrescriptionRequest {

    @NotNull(message = "Delivery method is required")
    private DeliveryMethod deliveryMethod;

    private String recipientEmail;

    private String recipientPhone;

    private String patientName;

    private String clinicName;

    private String clinicAddress;

    private String clinicPhone;
}


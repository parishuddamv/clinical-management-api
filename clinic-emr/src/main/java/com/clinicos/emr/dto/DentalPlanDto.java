package com.clinicos.emr.dto;

import com.clinicos.emr.entity.DentalTreatmentPlan;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DentalPlanDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String title;
    private DentalTreatmentPlan.Status status;
    /** Sum of the costs of items that are not cancelled (set by the server). */
    private BigDecimal totalAmount;
    /** Sum of the amounts already billed from this plan (set by the server). */
    private BigDecimal billedAmount;
    /** Earliest due date of lab work sent and not yet received (set by the server). */
    private LocalDate nextLabDue;
    /** { items: [...], notes, acceptedOn, ... } */
    private JsonNode data;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

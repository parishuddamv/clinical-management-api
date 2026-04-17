package com.clinicos.emr.dto;

import com.clinicos.emr.entity.LabReport.Interpretation;
import com.clinicos.emr.entity.LabReport.LabReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLabReportRequest {

    private Long visitId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private String reportNumber;

    private String testCategory;

    @NotBlank(message = "Test name is required")
    private String testName;

    @NotNull(message = "Test date is required")
    private LocalDate testDate;

    private LocalDate resultDate;

    private String labName;

    private String orderingDoctor;

    private String resultSummary;

    private Map<String, Object> resultValues;

    private Interpretation interpretation;

    private String referenceRange;

    private Boolean isAbnormal;

    private String notes;

    private LabReportStatus status;
}

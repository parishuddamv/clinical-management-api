package com.clinicos.emr.dto;

import com.clinicos.emr.entity.LabReport.Interpretation;
import com.clinicos.emr.entity.LabReport.LabReportStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabReportResponse {
    private Long id;
    private Long visitId;
    private Long patientId;
    private String reportNumber;
    private String testCategory;
    private String testName;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


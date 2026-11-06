package com.clinicos.emr.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientSummaryResponse {
    /** Plain-text summary for the doctor to check. */
    private String summary;
    /** False when there was nothing on record, so no AI call was made. */
    private boolean aiGenerated;
    private String model;
    private LocalDateTime generatedAt;
    /** e.g. "8 visits, 3 prescriptions, 2 lab reports, 1 allergy" */
    private String basedOn;
}

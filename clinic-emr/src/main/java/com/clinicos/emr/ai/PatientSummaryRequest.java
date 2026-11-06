package com.clinicos.emr.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Age and sex come from the patient service via the screen; the name is never sent. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientSummaryRequest {
    private Integer age;
    private String sex;
}

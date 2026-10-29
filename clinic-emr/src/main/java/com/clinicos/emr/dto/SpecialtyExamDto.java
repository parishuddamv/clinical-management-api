package com.clinicos.emr.dto;

import com.clinicos.emr.entity.SpecialtyExam;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyExamDto {
    private Long id;
    private Long patientId;
    private Long visitId;
    private SpecialtyExam.ExamType examType;
    /** The chart; its fields depend on examType. */
    private JsonNode data;
    private String recordedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.clinicos.emr.controller;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.emr.dto.SpecialtyExamDto;
import com.clinicos.emr.entity.SpecialtyExam;
import com.clinicos.emr.service.SpecialtyExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Dental charts, eye exams and skin lesion records for a patient. */
@RestController
@RequestMapping("/api/v1/emr/specialty-exams")
@RequiredArgsConstructor
public class SpecialtyExamController {

    private final SpecialtyExamService service;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<SpecialtyExamDto>>> list(@PathVariable Long patientId,
                                                                   @RequestParam(required = false) SpecialtyExam.ExamType type,
                                                                   Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(service.list(clinicId(auth), patientId, type)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SpecialtyExamDto>> create(@RequestBody SpecialtyExamDto body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Chart saved", service.create(clinicId(auth), auth.getName(), body)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SpecialtyExamDto>> update(@PathVariable Long id, @RequestBody SpecialtyExamDto body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Chart updated", service.update(clinicId(auth), auth.getName(), id, body)));
    }

    private static String clinicId(Authentication auth) {
        return (String) auth.getDetails();
    }
}

package com.clinicos.emr.service;

import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.exception.ResourceNotFoundException;
import com.clinicos.emr.dto.SpecialtyExamDto;
import com.clinicos.emr.entity.SpecialtyExam;
import com.clinicos.emr.repository.SpecialtyExamRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dental charts, eye exams and skin lesion records. The chart is kept as JSON so new
 * specialties can be added without schema changes; the server checks size and type only.
 */
@Service
@RequiredArgsConstructor
public class SpecialtyExamService {

    static final int MAX_BYTES = 32 * 1024;
    /** Charts can be corrected for a day; after that a new chart is recorded, keeping history. */
    static final Duration EDIT_WINDOW = Duration.ofHours(24);

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SpecialtyExamRepository exams;

    @Transactional(readOnly = true)
    public List<SpecialtyExamDto> list(String clinicId, Long patientId, SpecialtyExam.ExamType type) {
        List<SpecialtyExam> rows = type == null
                ? exams.findTop50ByClinicIdAndPatientIdOrderByCreatedAtDesc(clinicId, patientId)
                : exams.findTop50ByClinicIdAndPatientIdAndExamTypeOrderByCreatedAtDesc(clinicId, patientId, type);
        return rows.stream().map(SpecialtyExamService::toDto).toList();
    }

    @Transactional
    public SpecialtyExamDto create(String clinicId, String user, SpecialtyExamDto input) {
        if (input.getPatientId() == null) throw bad("Choose a patient.");
        if (input.getExamType() == null) throw bad("Choose the chart type.");
        SpecialtyExam exam = SpecialtyExam.builder()
                .clinicId(clinicId)
                .patientId(input.getPatientId())
                .visitId(input.getVisitId())
                .examType(input.getExamType())
                .data(serialise(input.getData()))
                .recordedBy(who(user))
                .build();
        return toDto(exams.save(exam));
    }

    @Transactional
    public SpecialtyExamDto update(String clinicId, String user, Long id, SpecialtyExamDto input) {
        SpecialtyExam exam = exams.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Chart not found: " + id));
        if (exam.getCreatedAt().plus(EDIT_WINDOW).isBefore(LocalDateTime.now())) {
            throw new RegistrationException(HttpStatus.CONFLICT, "Charts older than a day can't be changed. Record a new chart instead.");
        }
        if (input.getExamType() != null && input.getExamType() != exam.getExamType()) throw bad("The chart type can't be changed.");
        exam.setData(serialise(input.getData()));
        exam.setUpdatedAt(LocalDateTime.now());
        exam.setRecordedBy(who(user));
        return toDto(exams.save(exam));
    }

    static String serialise(JsonNode data) {
        if (data == null || !data.isObject()) throw bad("The chart is empty.");
        String text = data.toString();
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw bad("The chart is too large. Shorten the notes.");
        return text;
    }

    static SpecialtyExamDto toDto(SpecialtyExam e) {
        JsonNode data;
        try {
            data = JSON.readTree(e.getData());
        } catch (JsonProcessingException ex) {
            data = JSON.createObjectNode();
        }
        return SpecialtyExamDto.builder()
                .id(e.getId()).patientId(e.getPatientId()).visitId(e.getVisitId()).examType(e.getExamType())
                .data(data).recordedBy(e.getRecordedBy()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private static String who(String user) {
        if (user == null || user.isBlank()) throw new RegistrationException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
        return user.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static RegistrationException bad(String message) {
        return new RegistrationException(HttpStatus.BAD_REQUEST, message);
    }
}

package com.clinicos.feedback.controller;

import com.clinicos.feedback.dto.FeedbackDTO;
import com.clinicos.feedback.entity.PatientFeedback.FeedbackStatus;
import com.clinicos.feedback.service.FeedbackService;
import com.clinicos.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackDTO>> submitFeedback(
            @Valid @RequestBody FeedbackDTO dto,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        FeedbackDTO response = feedbackService.submitFeedback(clinicId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Feedback submitted successfully", response));
    }

    @PostMapping("/token/{token}")
    public ResponseEntity<ApiResponse<FeedbackDTO>> submitFeedbackByToken(
            @PathVariable String token,
            @Valid @RequestBody FeedbackDTO dto) {
        FeedbackDTO response = feedbackService.submitFeedbackByToken(token, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Feedback submitted successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeedbackDTO>> getFeedback(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        FeedbackDTO response = feedbackService.getFeedback(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<ApiResponse<FeedbackDTO>> respondToFeedback(
            @PathVariable Long id,
            @RequestParam String response,
            @RequestParam String respondedBy,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        FeedbackDTO result = feedbackService.respondToFeedback(clinicId, id, response, respondedBy);
        return ResponseEntity.ok(ApiResponse.success("Response added", result));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FeedbackDTO>> updateStatus(
            @PathVariable Long id,
            @RequestParam FeedbackStatus status,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        FeedbackDTO response = feedbackService.updateStatus(clinicId, id, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FeedbackDTO>>> getAllFeedback(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<FeedbackDTO> results = status != null ?
                feedbackService.getFeedbackByStatus(clinicId, status, PageRequest.of(page, size)) :
                feedbackService.getAllFeedback(clinicId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<Page<FeedbackDTO>>> getDoctorFeedback(
            @PathVariable Long doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<FeedbackDTO> results = feedbackService.getDoctorFeedback(clinicId, doctorId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Page<FeedbackDTO>>> getPublicFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<FeedbackDTO> results = feedbackService.getPublicFeedback(clinicId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeedbackStats(Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Map<String, Object> stats = feedbackService.getFeedbackStats(clinicId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/doctor/{doctorId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorRatingSummary(
            @PathVariable Long doctorId,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Map<String, Object> summary = feedbackService.getDoctorRatingSummary(clinicId, doctorId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}


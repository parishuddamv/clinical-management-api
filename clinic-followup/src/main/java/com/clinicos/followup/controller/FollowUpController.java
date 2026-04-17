package com.clinicos.followup.controller;

import com.clinicos.followup.dto.CreateFollowUpRequest;
import com.clinicos.followup.dto.FollowUpResponse;
import com.clinicos.followup.dto.UpdateFollowUpRequest;
import com.clinicos.followup.entity.FollowUp.FollowUpStatus;
import com.clinicos.followup.service.FollowUpService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/followups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;

    /**
     * POST /api/v1/followups
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FollowUpResponse>> create(
            @Valid @RequestBody CreateFollowUpRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        FollowUpResponse response = followUpService.create(clinicId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Follow-up created successfully", response));
    }

    /**
     * GET /api/v1/followups/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FollowUpResponse>> getById(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        FollowUpResponse response = followUpService.getById(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/followups/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FollowUpResponse>> update(
            @PathVariable Long id,
            @RequestBody UpdateFollowUpRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        FollowUpResponse response = followUpService.update(clinicId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Follow-up updated successfully", response));
    }

    /**
     * GET /api/v1/followups?status=&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FollowUpResponse>>> getAll(
            @RequestParam(required = false) FollowUpStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<FollowUpResponse> results = followUpService.getAll(clinicId, status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * GET /api/v1/followups/pending - Get pending follow-ups for the dashboard
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<FollowUpResponse>>> getPendingFollowUps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<FollowUpResponse> results = followUpService.getPendingFollowUps(clinicId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * GET /api/v1/followups/overdue - Get overdue follow-ups for the dashboard
     */
    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<Page<FollowUpResponse>>> getOverdueFollowUps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<FollowUpResponse> results = followUpService.getOverdueFollowUps(clinicId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}

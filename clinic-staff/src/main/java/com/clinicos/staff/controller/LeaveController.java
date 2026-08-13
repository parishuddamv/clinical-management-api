package com.clinicos.staff.controller;

import com.clinicos.staff.dto.StaffLeaveDTO;
import com.clinicos.staff.service.LeaveService;
import com.clinicos.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/staff/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    public ResponseEntity<ApiResponse<StaffLeaveDTO>> applyLeave(
            @Valid @RequestBody StaffLeaveDTO dto,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        StaffLeaveDTO response = leaveService.applyLeave(clinicId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave applied successfully", response));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<StaffLeaveDTO>> approveLeave(
            @PathVariable Long id,
            @RequestParam Long approverId,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        StaffLeaveDTO response = leaveService.approveLeave(clinicId, id, approverId);
        return ResponseEntity.ok(ApiResponse.success("Leave approved", response));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<StaffLeaveDTO>> rejectLeave(
            @PathVariable Long id,
            @RequestParam Long approverId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        StaffLeaveDTO response = leaveService.rejectLeave(clinicId, id, approverId, reason);
        return ResponseEntity.ok(ApiResponse.success("Leave rejected", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelLeave(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        leaveService.cancelLeave(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success("Leave cancelled", null));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<ApiResponse<Page<StaffLeaveDTO>>> getStaffLeaves(
            @PathVariable Long staffId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<StaffLeaveDTO> results = leaveService.getStaffLeaves(clinicId, staffId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<StaffLeaveDTO>>> getPendingLeaves(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<StaffLeaveDTO> results = leaveService.getPendingLeaves(clinicId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<StaffLeaveDTO>>> getUpcomingLeaves(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<StaffLeaveDTO> leaves = leaveService.getUpcomingLeaves(clinicId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(leaves));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}


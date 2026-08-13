package com.clinicos.staff.controller;

import com.clinicos.staff.dto.StaffMemberDTO;
import com.clinicos.staff.entity.StaffMember.StaffRole;
import com.clinicos.staff.service.StaffService;
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

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    public ResponseEntity<ApiResponse<StaffMemberDTO>> createStaff(
            @Valid @RequestBody StaffMemberDTO dto,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        StaffMemberDTO response = staffService.createStaff(clinicId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff member created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffMemberDTO>> getStaff(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        StaffMemberDTO response = staffService.getStaff(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<StaffMemberDTO>> getStaffByEmail(
            @PathVariable String email,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        StaffMemberDTO response = staffService.getStaffByEmail(clinicId, email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffMemberDTO>> updateStaff(
            @PathVariable Long id,
            @RequestBody StaffMemberDTO dto,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        StaffMemberDTO response = staffService.updateStaff(clinicId, id, dto);
        return ResponseEntity.ok(ApiResponse.success("Staff member updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateStaff(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        staffService.deactivateStaff(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success("Staff member deactivated", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StaffMemberDTO>>> getAllStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<StaffMemberDTO> results = staffService.getAllStaff(clinicId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<Page<StaffMemberDTO>>> getStaffByRole(
            @PathVariable StaffRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<StaffMemberDTO> results = staffService.getStaffByRole(clinicId, role, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<StaffMemberDTO>>> getActiveDoctors(Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<StaffMemberDTO> doctors = staffService.getActiveDoctors(clinicId);
        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<StaffMemberDTO>>> searchStaff(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<StaffMemberDTO> results = staffService.searchStaff(clinicId, q, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/permissions/{role}")
    public ResponseEntity<ApiResponse<Set<String>>> getPermissions(
            @PathVariable String role,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Set<String> permissions = staffService.getPermissions(clinicId, role);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/check-permission")
    public ResponseEntity<ApiResponse<Boolean>> checkPermission(
            @RequestParam String role,
            @RequestParam String permission,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        boolean hasPermission = staffService.hasPermission(clinicId, role, permission);
        return ResponseEntity.ok(ApiResponse.success(hasPermission));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}


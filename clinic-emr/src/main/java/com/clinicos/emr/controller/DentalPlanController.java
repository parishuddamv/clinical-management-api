package com.clinicos.emr.controller;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.emr.dto.DentalPlanDto;
import com.clinicos.emr.dto.DentalPriceListDto;
import com.clinicos.emr.service.DentalPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dental treatment plans and the clinic's dental price list.
 * Lives under /specialty-exams so the gateway's existing SPECIALTY permission covers it.
 */
@RestController
@RequestMapping("/api/v1/emr/specialty-exams/dental")
@RequiredArgsConstructor
public class DentalPlanController {

    private final DentalPlanService service;

    @GetMapping("/plans/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<DentalPlanDto>>> listForPatient(@PathVariable Long patientId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(service.listForPatient(clinicId(auth), patientId)));
    }

    @GetMapping("/plans/open")
    public ResponseEntity<ApiResponse<List<DentalPlanDto>>> listOpen(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(service.listOpen(clinicId(auth))));
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<DentalPlanDto>> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(service.get(clinicId(auth), id)));
    }

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<DentalPlanDto>> create(@RequestBody DentalPlanDto body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Treatment plan saved", service.create(clinicId(auth), auth.getName(), body)));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<DentalPlanDto>> update(@PathVariable Long id, @RequestBody DentalPlanDto body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Treatment plan updated", service.update(clinicId(auth), auth.getName(), id, body)));
    }

    @GetMapping("/price-list")
    public ResponseEntity<ApiResponse<DentalPriceListDto>> getPriceList(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(service.getPriceList(clinicId(auth))));
    }

    @PutMapping("/price-list")
    public ResponseEntity<ApiResponse<DentalPriceListDto>> savePriceList(@RequestBody DentalPriceListDto body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Price list saved", service.savePriceList(clinicId(auth), auth.getName(), body)));
    }

    private static String clinicId(Authentication auth) {
        return (String) auth.getDetails();
    }
}

package com.clinicos.billing.controller;

import com.clinicos.billing.dto.CreateInvoiceRequest;
import com.clinicos.billing.dto.InvoiceResponse;
import com.clinicos.billing.dto.UpdateInvoiceRequest;
import com.clinicos.billing.entity.Invoice.InvoiceStatus;
import com.clinicos.billing.service.InvoiceService;
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
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * POST /api/v1/billing
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(
            @Valid @RequestBody CreateInvoiceRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        InvoiceResponse response = invoiceService.create(clinicId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created successfully", response));
    }

    /**
     * GET /api/v1/billing/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        InvoiceResponse response = invoiceService.getById(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/billing/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> update(
            @PathVariable Long id,
            @RequestBody UpdateInvoiceRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        InvoiceResponse response = invoiceService.update(clinicId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Invoice updated successfully", response));
    }

    /**
     * GET /api/v1/billing?status=&patientId=&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAll(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<InvoiceResponse> results = invoiceService.getAll(clinicId, status, patientId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * GET /api/v1/billing/unpaid - Get unpaid invoices for the dashboard
     */
    @GetMapping("/unpaid")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getUnpaidInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<InvoiceResponse> results = invoiceService.getUnpaidInvoices(clinicId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * GET /api/v1/billing/overdue - Get overdue invoices for the dashboard
     */
    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getOverdueInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<InvoiceResponse> results = invoiceService.getOverdueInvoices(clinicId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}

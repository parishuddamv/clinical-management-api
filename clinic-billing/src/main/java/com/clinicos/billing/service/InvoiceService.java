package com.clinicos.billing.service;

import com.clinicos.billing.dto.CreateInvoiceRequest;
import com.clinicos.billing.dto.InvoiceResponse;
import com.clinicos.billing.dto.UpdateInvoiceRequest;
import com.clinicos.billing.entity.Invoice;
import com.clinicos.billing.entity.Invoice.InvoiceStatus;
import com.clinicos.billing.repository.InvoiceRepository;
import com.clinicos.billing.subscription.model.FeatureCode;
import com.clinicos.billing.subscription.service.SubscriptionService;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public InvoiceResponse create(String clinicId, CreateInvoiceRequest request, Authentication authentication) {
        subscriptionService.enforceAccess(authentication, FeatureCode.BASIC_BILLING, "MANAGE_BILLING");
        String invoiceNumber = generateInvoiceNumber();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .patientId(request.getPatientId())
                .totalAmount(request.getTotalAmount())
                .paidAmount(BigDecimal.ZERO)
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .status(InvoiceStatus.DRAFT)
                .build();
        invoice.setClinicId(clinicId);
        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(String clinicId, Long id, Authentication authentication) {
        subscriptionService.enforceAccess(authentication, FeatureCode.BASIC_BILLING, "VIEW_BILLING");
        Invoice invoice = findByIdAndClinic(clinicId, id);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public InvoiceResponse update(String clinicId, Long id, UpdateInvoiceRequest request, Authentication authentication) {
        subscriptionService.enforceAccess(authentication, FeatureCode.BASIC_BILLING, "MANAGE_BILLING");
        Invoice invoice = findByIdAndClinic(clinicId, id);

        if (request.getStatus() != null) {
            invoice.setStatus(request.getStatus());
        }
        if (request.getPaidAmount() != null) {
            invoice.setPaidAmount(request.getPaidAmount());
            // Auto-update status based on paid amount
            if (request.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus(InvoiceStatus.PAID);
            } else if (request.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
            }
        }

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAll(String clinicId, InvoiceStatus status, Long patientId, Pageable pageable, Authentication authentication) {
        subscriptionService.enforceAccess(authentication, FeatureCode.BASIC_BILLING, "VIEW_BILLING");
        Page<Invoice> page;
        if (status != null && patientId != null) {
            page = invoiceRepository.findByClinicIdAndStatusAndPatientId(clinicId, status, patientId, pageable);
        } else if (status != null) {
            page = invoiceRepository.findByClinicIdAndStatus(clinicId, status, pageable);
        } else if (patientId != null) {
            page = invoiceRepository.findByClinicIdAndPatientId(clinicId, patientId, pageable);
        } else {
            page = invoiceRepository.findByClinicId(clinicId, pageable);
        }
        return page.map(InvoiceResponse::from);
    }

    /**
     * Get unpaid invoices for the clinic (not paid and not cancelled)
     */
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getUnpaidInvoices(String clinicId, Pageable pageable, Authentication authentication) {
        subscriptionService.enforceAccess(authentication, FeatureCode.BASIC_BILLING, "VIEW_BILLING");
        log.info("Fetching unpaid invoices for clinic: {}", clinicId);
        Page<Invoice> page = invoiceRepository.findUnpaidByClinicId(clinicId, pageable);
        return page.map(InvoiceResponse::from);
    }

    /**
     * Get overdue invoices for the clinic (unpaid and past due date)
     */
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getOverdueInvoices(String clinicId, Pageable pageable, Authentication authentication) {
        subscriptionService.enforceAccess(authentication, FeatureCode.BASIC_BILLING, "VIEW_BILLING");
        LocalDate today = LocalDate.now();
        log.info("Fetching overdue invoices for clinic: {} (before {})", clinicId, today);
        Page<Invoice> page = invoiceRepository.findOverdueByClinicId(clinicId, today, pageable);
        return page.map(InvoiceResponse::from);
    }

    private Invoice findByIdAndClinic(String clinicId, Long id) {
        return invoiceRepository.findById(id)
                .filter(i -> clinicId.equals(i.getClinicId()))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    private String generateInvoiceNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "INV-" + date + "-" + System.currentTimeMillis() % 10000;
    }
}

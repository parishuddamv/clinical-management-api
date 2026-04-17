package com.clinicos.billing.dto;

import com.clinicos.billing.entity.Invoice;
import com.clinicos.billing.entity.Invoice.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long patientId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InvoiceResponse from(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .patientId(invoice.getPatientId())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .notes(invoice.getNotes())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}

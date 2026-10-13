package com.clinicos.billing.dto;

import com.clinicos.billing.entity.Invoice;
import com.clinicos.billing.entity.Invoice.InvoiceStatus;
import com.clinicos.billing.entity.InvoiceItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long patientId;

    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;

    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private String notes;

    private List<InvoiceItemResponse> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InvoiceResponse from(Invoice invoice) {
        List<InvoiceItemResponse> items = invoice.getItems() == null
                ? List.of()
                : invoice.getItems()
                    .stream()
                    .map(InvoiceItemResponse::from)
                    .toList();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .patientId(invoice.getPatientId())
                .subtotalAmount(invoice.getSubtotalAmount())
                .discountAmount(invoice.getDiscountAmount())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .notes(invoice.getNotes())
                .items(items)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}

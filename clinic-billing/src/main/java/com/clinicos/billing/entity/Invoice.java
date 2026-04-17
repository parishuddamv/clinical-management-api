package com.clinicos.billing.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Invoice entity for billing and invoicing
 */
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_clinic_patient", columnList = "clinic_id, patient_id"),
    @Index(name = "idx_clinic_status", columnList = "clinic_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Column(nullable = false, unique = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum InvoiceStatus {
        DRAFT, ISSUED, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED
    }
}


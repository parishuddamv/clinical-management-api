package com.clinicos.billing.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_clinic_invoice", columnList = "clinic_id, invoice_id"),
        @Index(name = "idx_payments_invoice_date", columnList = "invoice_id, payment_date"),
        @Index(name = "idx_payments_clinic_date", columnList = "clinic_id, payment_date"),
        @Index(name = "idx_payments_transaction_reference", columnList = "clinic_id, transaction_reference"),
        @Index(name = "uk_payments_clinic_payment_number", columnList = "clinic_id, payment_number", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "payment_number", nullable = false, length = 50)
    private String paymentNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    @Builder.Default
    private LocalDateTime paymentDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.COMPLETED;

    public enum PaymentMethod {
        CASH,
        UPI,
        CARD,
        BANK_TRANSFER,
        CHEQUE,
        OTHER
    }

    public enum PaymentStatus {
        COMPLETED,
        PENDING,
        FAILED,
        REFUNDED,
        CANCELLED
    }
}

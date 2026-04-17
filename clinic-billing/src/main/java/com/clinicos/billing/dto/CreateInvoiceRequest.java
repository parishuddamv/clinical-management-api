package com.clinicos.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateInvoiceRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    private String notes;
}

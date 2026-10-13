package com.clinicos.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateInvoiceRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    private String notes;

    @NotEmpty(message = "At least one invoice item is required")
    @Valid
    private List<CreateInvoiceItemRequest> items;
}

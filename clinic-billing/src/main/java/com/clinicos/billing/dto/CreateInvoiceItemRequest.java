package com.clinicos.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateInvoiceItemRequest {

    @NotBlank(message = "Item description is required")
    private String description;

    private String itemType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity = BigDecimal.ONE;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Tax percentage cannot be negative")
    private BigDecimal taxPercent = BigDecimal.ZERO;
}

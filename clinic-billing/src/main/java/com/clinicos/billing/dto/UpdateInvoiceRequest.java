package com.clinicos.billing.dto;

import com.clinicos.billing.entity.Invoice.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateInvoiceRequest {

    private InvoiceStatus status;

    private BigDecimal paidAmount;
}

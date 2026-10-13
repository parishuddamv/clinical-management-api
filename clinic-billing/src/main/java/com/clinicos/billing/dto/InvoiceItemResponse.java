package com.clinicos.billing.dto;

import com.clinicos.billing.entity.InvoiceItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceItemResponse {

    private Long id;
    private Long invoiceId;
    private String description;
    private String itemType;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal taxPercent;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InvoiceItemResponse from(InvoiceItem item) {
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .invoiceId(
                        item.getInvoice() != null
                                ? item.getInvoice().getId()
                                : null)
                .description(item.getDescription())
                .itemType(item.getItemType())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discountAmount(item.getDiscountAmount())
                .taxPercent(item.getTaxPercent())
                .taxAmount(item.getTaxAmount())
                .totalAmount(item.getTotalAmount())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}

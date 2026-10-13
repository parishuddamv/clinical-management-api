package com.clinicos.billing.service;

import com.clinicos.billing.dto.CreateInvoiceItemRequest;
import com.clinicos.billing.dto.CreateInvoiceRequest;
import com.clinicos.billing.dto.InvoiceResponse;
import com.clinicos.billing.dto.UpdateInvoiceRequest;
import com.clinicos.billing.entity.Invoice;
import com.clinicos.billing.entity.Invoice.InvoiceStatus;
import com.clinicos.billing.entity.InvoiceItem;
import com.clinicos.billing.repository.InvoiceRepository;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public InvoiceResponse create(String clinicId, CreateInvoiceRequest request) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one invoice item is required");
        }

        String invoiceNumber = generateInvoiceNumber();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .patientId(request.getPatientId())
                .subtotalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .status(InvoiceStatus.DRAFT)
                .build();

        invoice.setClinicId(clinicId);

        for (CreateInvoiceItemRequest requestItem : request.getItems()) {

            BigDecimal quantity = requestItem.getQuantity() == null
                    ? BigDecimal.ONE
                    : requestItem.getQuantity();

            BigDecimal unitPrice = requestItem.getUnitPrice();
            BigDecimal discount = requestItem.getDiscountAmount() == null
                    ? BigDecimal.ZERO
                    : requestItem.getDiscountAmount();

            BigDecimal taxPercent = requestItem.getTaxPercent() == null
                    ? BigDecimal.ZERO
                    : requestItem.getTaxPercent();

            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Item quantity must be greater than zero");
            }

            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Item unit price cannot be negative");
            }

            if (discount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Item discount cannot be negative");
            }

            if (taxPercent.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Item tax percentage cannot be negative");
            }

            BigDecimal lineSubtotal = quantity
                    .multiply(unitPrice)
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            if (discount.compareTo(lineSubtotal) > 0) {
                throw new IllegalArgumentException(
                        "Item discount cannot exceed item subtotal");
            }

            BigDecimal taxableAmount = lineSubtotal
                    .subtract(discount)
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            BigDecimal taxAmount = taxableAmount
                    .multiply(taxPercent)
                    .divide(BigDecimal.valueOf(100), MONEY_SCALE, MONEY_ROUNDING);

            BigDecimal lineTotal = taxableAmount
                    .add(taxAmount)
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .description(requestItem.getDescription().trim())
                    .itemType(requestItem.getItemType())
                    .quantity(quantity)
                    .unitPrice(unitPrice.setScale(MONEY_SCALE, MONEY_ROUNDING))
                    .discountAmount(discount.setScale(MONEY_SCALE, MONEY_ROUNDING))
                    .taxPercent(taxPercent.setScale(MONEY_SCALE, MONEY_ROUNDING))
                    .taxAmount(taxAmount)
                    .totalAmount(lineTotal)
                    .build();

            item.setClinicId(clinicId);
            invoice.getItems().add(item);

            subtotal = subtotal.add(lineSubtotal);
            discountTotal = discountTotal.add(discount);
            taxTotal = taxTotal.add(taxAmount);
        }

        subtotal = subtotal.setScale(MONEY_SCALE, MONEY_ROUNDING);
        discountTotal = discountTotal.setScale(MONEY_SCALE, MONEY_ROUNDING);
        taxTotal = taxTotal.setScale(MONEY_SCALE, MONEY_ROUNDING);

        BigDecimal total = subtotal
                .subtract(discountTotal)
                .add(taxTotal)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);

        invoice.setSubtotalAmount(subtotal);
        invoice.setDiscountAmount(discountTotal);
        invoice.setTaxAmount(taxTotal);
        invoice.setTotalAmount(total);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        return InvoiceResponse.from(savedInvoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(String clinicId, Long id) {
        Invoice invoice = findByIdAndClinic(clinicId, id);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public InvoiceResponse update(String clinicId, Long id, UpdateInvoiceRequest request) {
        Invoice invoice = findByIdAndClinic(clinicId, id);

        if (request.getStatus() != null) {
            invoice.setStatus(request.getStatus());
        }

        if (request.getPaidAmount() != null) {
            invoice.setPaidAmount(request.getPaidAmount());

            if (request.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus(InvoiceStatus.PAID);
            } else if (request.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
            }
        }

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAll(
            String clinicId,
            InvoiceStatus status,
            Long patientId,
            Pageable pageable) {

        Page<Invoice> page;

        if (status != null && patientId != null) {
            page = invoiceRepository.findByClinicIdAndStatusAndPatientId(
                    clinicId, status, patientId, pageable);
        } else if (status != null) {
            page = invoiceRepository.findByClinicIdAndStatus(
                    clinicId, status, pageable);
        } else if (patientId != null) {
            page = invoiceRepository.findByClinicIdAndPatientId(
                    clinicId, patientId, pageable);
        } else {
            page = invoiceRepository.findByClinicId(clinicId, pageable);
        }

        return page.map(InvoiceResponse::from);
    }

    /**
     * Get unpaid invoices for the clinic (not paid and not cancelled).
     */
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getUnpaidInvoices(
            String clinicId,
            Pageable pageable) {

        log.info("Fetching unpaid invoices for clinic: {}", clinicId);

        Page<Invoice> page =
                invoiceRepository.findUnpaidByClinicId(clinicId, pageable);

        return page.map(InvoiceResponse::from);
    }

    /**
     * Get overdue invoices for the clinic (unpaid and past due date).
     */
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getOverdueInvoices(
            String clinicId,
            Pageable pageable) {

        LocalDate today = LocalDate.now();

        log.info(
                "Fetching overdue invoices for clinic: {} (before {})",
                clinicId,
                today);

        Page<Invoice> page =
                invoiceRepository.findOverdueByClinicId(
                        clinicId,
                        today,
                        pageable);

        return page.map(InvoiceResponse::from);
    }

    private Invoice findByIdAndClinic(String clinicId, Long id) {
        return invoiceRepository.findById(id)
                .filter(i -> clinicId.equals(i.getClinicId()))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found: " + id));
    }

    private String generateInvoiceNumber() {
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return "INV-" + date + "-" +
                System.currentTimeMillis() % 10000;
    }
}

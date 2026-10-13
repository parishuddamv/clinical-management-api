package com.clinicos.billing.service;

import com.clinicos.billing.dto.CreatePaymentRequest;
import com.clinicos.billing.dto.PaymentResponse;
import com.clinicos.billing.entity.Invoice;
import com.clinicos.billing.entity.Invoice.InvoiceStatus;
import com.clinicos.billing.entity.Payment;
import com.clinicos.billing.repository.InvoiceRepository;
import com.clinicos.billing.repository.PaymentRepository;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public PaymentResponse recordPayment(
            String clinicId,
            Long invoiceId,
            CreatePaymentRequest request) {

        Invoice invoice = invoiceRepository
                .findByIdAndClinicIdForUpdate(invoiceId, clinicId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot record payment for a cancelled invoice");
        }

        if (request.getAmount() == null ||
                request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero");
        }

        BigDecimal paidAmount = invoice.getPaidAmount() == null
                ? BigDecimal.ZERO
                : invoice.getPaidAmount();

        BigDecimal totalAmount = invoice.getTotalAmount();

        BigDecimal outstandingAmount =
                totalAmount.subtract(paidAmount);

        if (request.getAmount().compareTo(outstandingAmount) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount cannot exceed outstanding balance of "
                            + outstandingAmount);
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .paymentNumber(generatePaymentNumber())
                .amount(request.getAmount())
                .paymentDate(
                        request.getPaymentDate() != null
                                ? request.getPaymentDate()
                                : LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .transactionReference(request.getTransactionReference())
                .notes(request.getNotes())
                .status(Payment.PaymentStatus.COMPLETED)
                .build();

        payment.setClinicId(clinicId);

        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal newPaidAmount =
                paidAmount.add(request.getAmount());

        invoice.setPaidAmount(newPaidAmount);

        if (newPaidAmount.compareTo(totalAmount) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoiceRepository.save(invoice);

        log.info(
                "Payment recorded: clinicId={}, invoiceId={}, amount={}, paymentNumber={}",
                clinicId,
                invoiceId,
                request.getAmount(),
                savedPayment.getPaymentNumber());

        return PaymentResponse.from(savedPayment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(
            String clinicId,
            Long invoiceId,
            Pageable pageable) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .filter(i -> clinicId.equals(i.getClinicId()))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found: " + invoiceId));

        return paymentRepository
                .findByClinicIdAndInvoiceId(
                        clinicId,
                        invoice.getId(),
                        pageable)
                .map(PaymentResponse::from);
    }

    private String generatePaymentNumber() {
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        return "PAY-" + date + "-" +
                (System.currentTimeMillis() % 10000);
    }
}

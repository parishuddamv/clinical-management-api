package com.clinicos.billing.repository;

import com.clinicos.billing.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByClinicIdAndInvoiceId(
            String clinicId,
            Long invoiceId,
            Pageable pageable
    );

    List<Payment> findByClinicIdAndInvoiceIdOrderByPaymentDateDesc(
            String clinicId,
            Long invoiceId
    );
}

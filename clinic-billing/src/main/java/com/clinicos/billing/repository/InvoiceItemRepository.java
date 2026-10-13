package com.clinicos.billing.repository;

import com.clinicos.billing.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoiceIdAndClinicId(Long invoiceId, String clinicId);

    void deleteByInvoiceIdAndClinicId(Long invoiceId, String clinicId);
}

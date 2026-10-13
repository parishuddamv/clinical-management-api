package com.clinicos.billing.repository;

import com.clinicos.billing.entity.Invoice;
import com.clinicos.billing.entity.Invoice.InvoiceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findByClinicId(String clinicId, Pageable pageable);

    Page<Invoice> findByClinicIdAndStatus(
            String clinicId,
            InvoiceStatus status,
            Pageable pageable);

    Page<Invoice> findByClinicIdAndPatientId(
            String clinicId,
            Long patientId,
            Pageable pageable);

    Page<Invoice> findByClinicIdAndStatusAndPatientId(
            String clinicId,
            InvoiceStatus status,
            Long patientId,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM Invoice i
            WHERE i.id = :id
              AND i.clinicId = :clinicId
            """)
    Optional<Invoice> findByIdAndClinicIdForUpdate(
            @Param("id") Long id,
            @Param("clinicId") String clinicId);

    /**
     * Find unpaid invoices (status NOT PAID and NOT CANCELLED)
     */
    @Query("SELECT i FROM Invoice i WHERE i.clinicId = :clinicId " +
           "AND i.status NOT IN ('PAID', 'CANCELLED') ORDER BY i.dueDate ASC")
    Page<Invoice> findUnpaidByClinicId(
            @Param("clinicId") String clinicId,
            Pageable pageable);

    /**
     * Find overdue invoices (unpaid and dueDate < today)
     */
    @Query("SELECT i FROM Invoice i WHERE i.clinicId = :clinicId " +
           "AND i.status NOT IN ('PAID', 'CANCELLED') " +
           "AND i.dueDate < :today ORDER BY i.dueDate ASC")
    Page<Invoice> findOverdueByClinicId(
            @Param("clinicId") String clinicId,
            @Param("today") LocalDate today,
            Pageable pageable);
}

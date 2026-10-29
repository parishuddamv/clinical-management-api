package com.clinicos.common.repository;

import com.clinicos.common.entity.BookingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {

    Page<BookingRequest> findByClinicIdAndStatusOrderByPreferredDateAscCreatedAtAsc(String clinicId, BookingRequest.Status status, Pageable pageable);

    Page<BookingRequest> findByClinicIdOrderByCreatedAtDesc(String clinicId, Pageable pageable);

    Optional<BookingRequest> findByIdAndClinicId(Long id, String clinicId);

    long countByClinicIdAndPhoneAndCreatedAtAfter(String clinicId, String phone, LocalDateTime since);

    long countByRequesterIpAndCreatedAtAfter(String requesterIp, LocalDateTime since);

    long countByClinicIdAndStatus(String clinicId, BookingRequest.Status status);

    boolean existsByReference(String reference);
}

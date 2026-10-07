package com.clinicos.staff.repository;

import com.clinicos.staff.entity.StaffRequest;
import com.clinicos.staff.entity.StaffRequest.StaffRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffRequestRepository extends JpaRepository<StaffRequest, Long> {

    Page<StaffRequest> findByClinicIdOrderByCreatedAtDesc(
            String clinicId,
            Pageable pageable
    );

    Page<StaffRequest> findByClinicIdAndStatusOrderByCreatedAtDesc(
            String clinicId,
            StaffRequestStatus status,
            Pageable pageable
    );

    Page<StaffRequest> findByStatusOrderByCreatedAtDesc(
            StaffRequestStatus status,
            Pageable pageable
    );

    boolean existsByClinicIdAndEmailIgnoreCaseAndStatus(
            String clinicId,
            String email,
            StaffRequestStatus status
    );

    long countByClinicIdAndStatus(
            String clinicId,
            StaffRequestStatus status
    );

    long countByStatus(
            StaffRequestStatus status
    );
}
package com.clinicos.followup.repository;

import com.clinicos.followup.entity.FollowUp;
import com.clinicos.followup.entity.FollowUp.FollowUpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    Page<FollowUp> findByClinicId(String clinicId, Pageable pageable);

    Page<FollowUp> findByClinicIdAndStatus(String clinicId, FollowUpStatus status, Pageable pageable);

    /**
     * Find pending follow-ups (status = PENDING)
     */
    @Query("SELECT f FROM FollowUp f WHERE f.clinicId = :clinicId AND f.status = 'PENDING' ORDER BY f.dueDate ASC")
    Page<FollowUp> findPendingByClinicId(@Param("clinicId") String clinicId, Pageable pageable);

    /**
     * Find overdue follow-ups (status = PENDING and dueDate < today)
     */
    @Query("SELECT f FROM FollowUp f WHERE f.clinicId = :clinicId AND f.status = 'PENDING' AND f.dueDate < :today ORDER BY f.dueDate ASC")
    Page<FollowUp> findOverdueByClinicId(@Param("clinicId") String clinicId, @Param("today") LocalDate today, Pageable pageable);
}

package com.clinicos.staff.repository;

import com.clinicos.staff.entity.StaffLeave;
import com.clinicos.staff.entity.StaffLeave.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffLeaveRepository extends JpaRepository<StaffLeave, Long> {

    Optional<StaffLeave> findByIdAndClinicId(Long id, String clinicId);

    Page<StaffLeave> findByStaffIdOrderByStartDateDesc(Long staffId, Pageable pageable);

    Page<StaffLeave> findByClinicIdAndStatusOrderByStartDateDesc(String clinicId, LeaveStatus status, Pageable pageable);

    @Query("SELECT l FROM StaffLeave l WHERE l.staff.id = :staffId AND l.status = 'APPROVED' " +
           "AND :date BETWEEN l.startDate AND l.endDate")
    List<StaffLeave> findApprovedLeavesForDate(@Param("staffId") Long staffId, @Param("date") LocalDate date);

    @Query("SELECT l FROM StaffLeave l WHERE l.clinicId = :clinicId AND l.status = 'APPROVED' " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate ORDER BY l.startDate")
    List<StaffLeave> findApprovedLeavesInRange(
            @Param("clinicId") String clinicId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    long countByClinicIdAndStatus(String clinicId, LeaveStatus status);
}


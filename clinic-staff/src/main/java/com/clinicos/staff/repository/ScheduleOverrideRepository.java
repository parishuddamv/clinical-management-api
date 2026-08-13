package com.clinicos.staff.repository;

import com.clinicos.staff.entity.ScheduleOverride;
import com.clinicos.staff.entity.ScheduleOverride.OverrideType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleOverrideRepository extends JpaRepository<ScheduleOverride, Long> {

    Optional<ScheduleOverride> findByIdAndClinicId(Long id, String clinicId);

    Optional<ScheduleOverride> findByStaffIdAndOverrideDate(Long staffId, LocalDate date);

    List<ScheduleOverride> findByStaffIdAndOverrideDateBetweenOrderByOverrideDateAsc(
            Long staffId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT o FROM ScheduleOverride o WHERE o.clinicId = :clinicId " +
           "AND o.overrideDate BETWEEN :startDate AND :endDate ORDER BY o.overrideDate")
    List<ScheduleOverride> findClinicOverrides(
            @Param("clinicId") String clinicId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByStaffIdAndOverrideDate(Long staffId, LocalDate date);
}


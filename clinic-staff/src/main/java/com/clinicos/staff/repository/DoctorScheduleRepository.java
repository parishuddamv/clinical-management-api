package com.clinicos.staff.repository;

import com.clinicos.staff.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    Optional<DoctorSchedule> findByIdAndClinicId(Long id, String clinicId);

    List<DoctorSchedule> findByStaffIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(Long staffId);

    List<DoctorSchedule> findByClinicIdAndIsActiveTrueOrderByDayOfWeekAsc(String clinicId);

    @Query("SELECT s FROM DoctorSchedule s WHERE s.staff.id = :staffId AND s.dayOfWeek = :dayOfWeek " +
           "AND s.isActive = true AND (s.effectiveFrom IS NULL OR s.effectiveFrom <= :date) " +
           "AND (s.effectiveUntil IS NULL OR s.effectiveUntil >= :date)")
    List<DoctorSchedule> findActiveScheduleForDay(
            @Param("staffId") Long staffId,
            @Param("dayOfWeek") int dayOfWeek,
            @Param("date") LocalDate date);

    @Query("SELECT s FROM DoctorSchedule s WHERE s.clinicId = :clinicId AND s.dayOfWeek = :dayOfWeek " +
           "AND s.isActive = true ORDER BY s.staff.firstName")
    List<DoctorSchedule> findClinicScheduleForDay(@Param("clinicId") String clinicId, @Param("dayOfWeek") int dayOfWeek);

    void deleteByStaffId(Long staffId);

    boolean existsByStaffIdAndDayOfWeekAndIsActiveTrue(Long staffId, int dayOfWeek);
}


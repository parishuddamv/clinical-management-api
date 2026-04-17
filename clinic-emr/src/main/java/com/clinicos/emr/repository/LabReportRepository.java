package com.clinicos.emr.repository;

import com.clinicos.emr.entity.LabReport;
import com.clinicos.emr.entity.LabReport.LabReportStatus;
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
public interface LabReportRepository extends JpaRepository<LabReport, Long> {

    Optional<LabReport> findByIdAndClinicId(Long id, String clinicId);

    Page<LabReport> findByClinicIdOrderByTestDateDesc(String clinicId, Pageable pageable);

    Page<LabReport> findByClinicIdAndPatientIdOrderByTestDateDesc(
            String clinicId, Long patientId, Pageable pageable);

    List<LabReport> findByVisitIdOrderByTestDateDesc(Long visitId);

    Page<LabReport> findByClinicIdAndStatusOrderByTestDateDesc(
            String clinicId, LabReportStatus status, Pageable pageable);

    @Query("SELECT l FROM LabReport l WHERE l.clinicId = :clinicId " +
           "AND l.testDate BETWEEN :startDate AND :endDate " +
           "ORDER BY l.testDate DESC")
    Page<LabReport> findByClinicIdAndDateRange(
            @Param("clinicId") String clinicId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT l FROM LabReport l WHERE l.clinicId = :clinicId " +
           "AND l.patientId = :patientId AND l.isAbnormal = true " +
           "ORDER BY l.testDate DESC")
    List<LabReport> findAbnormalReports(@Param("clinicId") String clinicId, @Param("patientId") Long patientId);

    @Query("SELECT DISTINCT l.testCategory FROM LabReport l WHERE l.clinicId = :clinicId " +
           "AND l.testCategory IS NOT NULL ORDER BY l.testCategory")
    List<String> findAllTestCategories(@Param("clinicId") String clinicId);
}


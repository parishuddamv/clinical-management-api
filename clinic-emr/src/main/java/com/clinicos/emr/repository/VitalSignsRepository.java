package com.clinicos.emr.repository;

import com.clinicos.emr.entity.VitalSigns;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VitalSignsRepository extends JpaRepository<VitalSigns, Long> {

    Optional<VitalSigns> findByIdAndClinicId(Long id, String clinicId);

    Page<VitalSigns> findByClinicIdAndPatientIdOrderByRecordedAtDesc(
            String clinicId, Long patientId, Pageable pageable);

    List<VitalSigns> findByVisitIdOrderByRecordedAtDesc(Long visitId);

    @Query("SELECT v FROM VitalSigns v WHERE v.clinicId = :clinicId AND v.patientId = :patientId " +
           "ORDER BY v.recordedAt DESC LIMIT 1")
    Optional<VitalSigns> findLatestByPatient(@Param("clinicId") String clinicId, @Param("patientId") Long patientId);

    @Query("SELECT v FROM VitalSigns v WHERE v.clinicId = :clinicId AND v.patientId = :patientId " +
           "AND v.recordedAt BETWEEN :startDate AND :endDate ORDER BY v.recordedAt ASC")
    List<VitalSigns> findByPatientAndDateRange(
            @Param("clinicId") String clinicId,
            @Param("patientId") Long patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}


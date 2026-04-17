package com.clinicos.emr.repository;

import com.clinicos.emr.entity.PatientVisit;
import com.clinicos.emr.entity.PatientVisit.VisitStatus;
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
public interface PatientVisitRepository extends JpaRepository<PatientVisit, Long> {

    Optional<PatientVisit> findByIdAndClinicId(Long id, String clinicId);

    Page<PatientVisit> findByClinicIdOrderByVisitDateTimeDesc(String clinicId, Pageable pageable);

    Page<PatientVisit> findByClinicIdAndPatientIdOrderByVisitDateTimeDesc(
            String clinicId, Long patientId, Pageable pageable);

    Page<PatientVisit> findByClinicIdAndDoctorIdOrderByVisitDateTimeDesc(
            String clinicId, String doctorId, Pageable pageable);

    Page<PatientVisit> findByClinicIdAndStatusOrderByVisitDateTimeDesc(
            String clinicId, VisitStatus status, Pageable pageable);

    @Query("SELECT v FROM PatientVisit v WHERE v.clinicId = :clinicId " +
           "AND v.visitDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY v.visitDateTime DESC")
    Page<PatientVisit> findByClinicIdAndDateRange(
            @Param("clinicId") String clinicId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT v FROM PatientVisit v LEFT JOIN FETCH v.diagnoses " +
           "LEFT JOIN FETCH v.prescriptions WHERE v.id = :id AND v.clinicId = :clinicId")
    Optional<PatientVisit> findByIdWithDetailsAndClinicId(@Param("id") Long id, @Param("clinicId") String clinicId);

    List<PatientVisit> findByClinicIdAndPatientIdAndStatusOrderByVisitDateTimeDesc(
            String clinicId, Long patientId, VisitStatus status);

    long countByClinicIdAndPatientId(String clinicId, Long patientId);
}


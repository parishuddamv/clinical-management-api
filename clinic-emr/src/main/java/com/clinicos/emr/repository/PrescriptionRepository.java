package com.clinicos.emr.repository;

import com.clinicos.emr.entity.Prescription;
import com.clinicos.emr.entity.Prescription.PrescriptionStatus;
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
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByIdAndClinicId(Long id, String clinicId);

    Optional<Prescription> findByPrescriptionNumberAndClinicId(String prescriptionNumber, String clinicId);

    @Query("SELECT p FROM Prescription p LEFT JOIN FETCH p.items WHERE p.id = :id AND p.clinicId = :clinicId")
    Optional<Prescription> findByIdWithItemsAndClinicId(@Param("id") Long id, @Param("clinicId") String clinicId);

    Page<Prescription> findByClinicIdOrderByCreatedAtDesc(String clinicId, Pageable pageable);

    Page<Prescription> findByClinicIdAndPatientIdOrderByPrescriptionDateDesc(
            String clinicId, Long patientId, Pageable pageable);

    Page<Prescription> findByClinicIdAndStatusOrderByCreatedAtDesc(
            String clinicId, PrescriptionStatus status, Pageable pageable);

    List<Prescription> findByVisitIdOrderByCreatedAtDesc(Long visitId);

    @Query("SELECT p FROM Prescription p WHERE p.clinicId = :clinicId " +
           "AND p.prescriptionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.prescriptionDate DESC")
    Page<Prescription> findByClinicIdAndDateRange(
            @Param("clinicId") String clinicId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.prescriptionNumber, LENGTH(:prefix) + 1) AS long)), 0) " +
           "FROM Prescription p WHERE p.clinicId = :clinicId AND p.prescriptionNumber LIKE CONCAT(:prefix, '%')")
    Long findMaxPrescriptionNumber(@Param("clinicId") String clinicId, @Param("prefix") String prefix);

    long countByClinicIdAndPrescriptionDate(String clinicId, LocalDate date);
}


package com.clinicos.emr.repository;

import com.clinicos.emr.entity.Diagnosis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    Optional<Diagnosis> findByIdAndClinicId(Long id, String clinicId);

    List<Diagnosis> findByVisitIdOrderByDiagnosisTypeAsc(Long visitId);

    Page<Diagnosis> findByClinicIdAndPatientIdOrderByCreatedAtDesc(
            String clinicId, Long patientId, Pageable pageable);

    @Query("SELECT d FROM Diagnosis d WHERE d.clinicId = :clinicId AND d.patientId = :patientId " +
           "AND d.isChronic = true ORDER BY d.createdAt DESC")
    List<Diagnosis> findChronicDiagnoses(@Param("clinicId") String clinicId, @Param("patientId") Long patientId);

    @Query("SELECT DISTINCT d.diagnosisName FROM Diagnosis d WHERE d.clinicId = :clinicId " +
           "AND LOWER(d.diagnosisName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> searchDiagnosisNames(@Param("clinicId") String clinicId, @Param("query") String query);

    @Query("SELECT d FROM Diagnosis d WHERE d.clinicId = :clinicId " +
           "AND d.icdCode = :icdCode ORDER BY d.createdAt DESC")
    List<Diagnosis> findByIcdCode(@Param("clinicId") String clinicId, @Param("icdCode") String icdCode);
}


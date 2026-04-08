package com.clinicos.patient.repository;

import com.clinicos.patient.entity.PatientTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PatientTag entity
 */
@Repository
public interface PatientTagRepository extends JpaRepository<PatientTag, Long> {

    /**
     * Find all tags for a patient in a clinic
     */
    List<PatientTag> findByClinicIdAndPatientId(String clinicId, Long patientId);

    /**
     * Find a specific tag for a patient
     */
    Optional<PatientTag> findByClinicIdAndPatientIdAndTag(String clinicId, Long patientId, String tag);

    /**
     * Check if tag exists for patient
     */
    boolean existsByClinicIdAndPatientIdAndTag(String clinicId, Long patientId, String tag);

    /**
     * Delete all tags for a patient
     */
    void deleteByClinicIdAndPatientId(String clinicId, Long patientId);
}


package com.clinicos.patient.repository;

import com.clinicos.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Patient entity with custom queries for clinic-scoped isolation.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Find patient by clinic and phone (unique per clinic)
     */
    Optional<Patient> findByClinicIdAndPhone(String clinicId, String phone);

    /**
     * Search patients by name (first name + last name) with pagination
     */
    @Query("SELECT p FROM Patient p WHERE p.clinicId = :clinicId AND " +
           "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Patient> searchByName(@Param("clinicId") String clinicId, @Param("query") String query, Pageable pageable);

    /**
     * Search patients by phone
     */
    @Query("SELECT p FROM Patient p WHERE p.clinicId = :clinicId AND LOWER(p.phone) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Patient> searchByPhone(@Param("clinicId") String clinicId, @Param("query") String query, Pageable pageable);

    /**
     * Find all active patients in clinic with pagination
     */
    Page<Patient> findByClinicIdAndIsActiveTrue(String clinicId, Pageable pageable);

    /**
     * Find patient by ID and clinic (security check)
     */
    Optional<Patient> findByIdAndClinicId(Long id, String clinicId);

    /**
     * Get all patients for a clinic
     */
    List<Patient> findByClinicId(String clinicId);
}


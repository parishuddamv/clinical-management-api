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

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByClinicIdAndPhone(String clinicId, String phone);

    /**
     * Search patients by name.
     *
     * IMPORTANT:
     * This query returns the complete Patient entity columns.
     * Do not select a partial column set when mapping directly to Patient.
     */
    @Query(value = """
        SELECT p.*
        FROM patients p
        WHERE p.clinic_id = :clinicId
          AND p.is_active = true
          AND (
              LOWER(p.first_name) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(p.last_name) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY p.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM patients p
        WHERE p.clinic_id = :clinicId
          AND p.is_active = true
          AND (
              LOWER(p.first_name) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(p.last_name) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """,
            nativeQuery = true)
    Page<Patient> searchByName(
            @Param("clinicId") String clinicId,
            @Param("query") String query,
            Pageable pageable);

    /**
     * Search patients by phone.
     */
    @Query(value = """
        SELECT p.*
        FROM patients p
        WHERE p.clinic_id = :clinicId
          AND p.is_active = true
          AND LOWER(p.phone) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY p.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM patients p
        WHERE p.clinic_id = :clinicId
          AND p.is_active = true
          AND LOWER(p.phone) LIKE LOWER(CONCAT('%', :query, '%'))
        """,
            nativeQuery = true)
    Page<Patient> searchByPhone(
            @Param("clinicId") String clinicId,
            @Param("query") String query,
            Pageable pageable);

    /**
     * Find all active patients in clinic with pagination.
     */
    @Query("""
        SELECT p
        FROM Patient p
        WHERE p.clinicId = :clinicId
          AND p.isActive = true
        """)
    Page<Patient> findByClinicIdAndIsActiveTrue(
            @Param("clinicId") String clinicId,
            Pageable pageable);

    /**
     * Find patient by ID and clinic.
     */
    Optional<Patient> findByIdAndClinicId(Long id, String clinicId);

    /**
     * Get all patients for a clinic.
     */
    List<Patient> findByClinicId(String clinicId);

    /**
     * Get active patient count.
     */
    @Query(
            value = """
            SELECT COUNT(*)
            FROM patients
            WHERE clinic_id = :clinicId
              AND is_active = true
            """,
            nativeQuery = true)
    Long countByClinicIdAndIsActiveTrue(
            @Param("clinicId") String clinicId);

    /**
     * Get recently registered patients.
     *
     * Uses Pageable to cap the number of rows returned.
     */
    @Query("""
        SELECT p
        FROM Patient p
        WHERE p.clinicId = :clinicId
          AND p.isActive = true
        ORDER BY p.createdAt DESC
        """)
    List<Patient> findRecentPatients(
            @Param("clinicId") String clinicId,
            Pageable pageable);
}
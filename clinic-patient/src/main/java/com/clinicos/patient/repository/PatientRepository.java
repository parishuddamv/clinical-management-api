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
 * Optimized Repository for Patient entity with performance-focused queries.
 * Uses native queries and projections for minimal data transfer.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Find patient by clinic and phone (unique per clinic)
     * Uses native query for optimal performance
     */
    Optional<Patient> findByClinicIdAndPhone(String clinicId, String phone);

    /**
     * Search patients by name with projection - OPTIMIZED
     * Only selects necessary columns for fast response
     */
    @Query(value = "SELECT p.id, p.first_name, p.last_name, p.phone, p.gender, p.is_active, p.created_at " +
           "FROM clinic_patient p " +
           "WHERE p.clinic_id = :clinicId " +
           "AND p.is_active = true " +
           "AND (LOWER(p.first_name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.last_name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM clinic_patient p " +
                       "WHERE p.clinic_id = :clinicId " +
                       "AND p.is_active = true " +
                       "AND (LOWER(p.first_name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                       "LOWER(p.last_name) LIKE LOWER(CONCAT('%', :query, '%')))",
           nativeQuery = true)
    Page<Patient> searchByName(@Param("clinicId") String clinicId, @Param("query") String query, Pageable pageable);

    /**
     * Search patients by phone - OPTIMIZED
     * Native query for faster execution
     */
    @Query(value = "SELECT p.id, p.first_name, p.last_name, p.phone, p.gender, p.is_active, p.created_at " +
           "FROM clinic_patient p " +
           "WHERE p.clinic_id = :clinicId " +
           "AND p.is_active = true " +
           "AND LOWER(p.phone) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY p.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM clinic_patient p " +
                       "WHERE p.clinic_id = :clinicId " +
                       "AND p.is_active = true " +
                       "AND LOWER(p.phone) LIKE LOWER(CONCAT('%', :query, '%'))",
           nativeQuery = true)
    Page<Patient> searchByPhone(@Param("clinicId") String clinicId, @Param("query") String query, Pageable pageable);

    /**
     * Find all active patients in clinic with pagination - OPTIMIZED
     */
    @Query("SELECT p FROM Patient p WHERE p.clinicId = :clinicId AND p.isActive = true")
    Page<Patient> findByClinicIdAndIsActiveTrue(@Param("clinicId") String clinicId, Pageable pageable);

    /**
     * Find patient by ID and clinic (security check)
     */
    Optional<Patient> findByIdAndClinicId(Long id, String clinicId);

    /**
     * Get all patients for a clinic
     */
    List<Patient> findByClinicId(String clinicId);

    /**
     * Get patient count by clinic - Fast count query
     */
    @Query(value = "SELECT COUNT(*) FROM clinic_patient WHERE clinic_id = :clinicId AND is_active = true", nativeQuery = true)
    Long countByClinicIdAndIsActiveTrue(@Param("clinicId") String clinicId);

    /**
     * Get recently registered patients for dashboard
     */
    @Query(value = "SELECT p.id, p.first_name, p.last_name, p.phone, p.gender, p.is_active, p.created_at " +
           "FROM clinic_patient p " +
           "WHERE p.clinic_id = :clinicId AND p.is_active = true " +
           "ORDER BY p.created_at DESC LIMIT :limit",
           nativeQuery = true)
    List<Patient> findRecentPatients(@Param("clinicId") String clinicId, @Param("limit") int limit);
}


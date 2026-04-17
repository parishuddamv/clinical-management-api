package com.clinicos.emr.repository;

import com.clinicos.emr.entity.PatientAllergy;
import com.clinicos.emr.entity.PatientAllergy.AllergenType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, Long> {

    Optional<PatientAllergy> findByIdAndClinicId(Long id, String clinicId);

    Page<PatientAllergy> findByClinicIdAndPatientIdOrderByCreatedAtDesc(
            String clinicId, Long patientId, Pageable pageable);

    List<PatientAllergy> findByClinicIdAndPatientIdAndIsActiveTrueOrderByAllergenTypeAsc(
            String clinicId, Long patientId);

    @Query("SELECT a FROM PatientAllergy a WHERE a.clinicId = :clinicId " +
           "AND a.patientId = :patientId AND a.allergenType = :type " +
           "ORDER BY a.createdAt DESC")
    List<PatientAllergy> findByPatientAndType(
            @Param("clinicId") String clinicId,
            @Param("patientId") Long patientId,
            @Param("type") AllergenType type);

    @Query("SELECT a FROM PatientAllergy a WHERE a.clinicId = :clinicId " +
           "AND a.patientId = :patientId AND a.allergenType = 'DRUG' " +
           "AND a.isActive = true ORDER BY a.allergenName ASC")
    List<PatientAllergy> findDrugAllergies(@Param("clinicId") String clinicId, @Param("patientId") Long patientId);

    @Query("SELECT COUNT(a) > 0 FROM PatientAllergy a WHERE a.clinicId = :clinicId " +
           "AND a.patientId = :patientId AND LOWER(a.allergenName) = LOWER(:allergenName)")
    boolean existsByPatientAndAllergen(@Param("clinicId") String clinicId,
                                        @Param("patientId") Long patientId,
                                        @Param("allergenName") String allergenName);
}

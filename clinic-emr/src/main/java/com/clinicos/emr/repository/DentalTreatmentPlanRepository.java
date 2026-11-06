package com.clinicos.emr.repository;

import com.clinicos.emr.entity.DentalTreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DentalTreatmentPlanRepository extends JpaRepository<DentalTreatmentPlan, Long> {

    List<DentalTreatmentPlan> findTop50ByClinicIdAndPatientIdOrderByCreatedAtDesc(String clinicId, Long patientId);

    List<DentalTreatmentPlan> findTop300ByClinicIdAndStatusNotOrderByUpdatedAtDesc(String clinicId, DentalTreatmentPlan.Status status);

    Optional<DentalTreatmentPlan> findByIdAndClinicId(Long id, String clinicId);
}

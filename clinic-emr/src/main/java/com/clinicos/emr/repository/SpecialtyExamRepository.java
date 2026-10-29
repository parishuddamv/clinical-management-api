package com.clinicos.emr.repository;

import com.clinicos.emr.entity.SpecialtyExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialtyExamRepository extends JpaRepository<SpecialtyExam, Long> {

    List<SpecialtyExam> findTop50ByClinicIdAndPatientIdOrderByCreatedAtDesc(String clinicId, Long patientId);

    List<SpecialtyExam> findTop50ByClinicIdAndPatientIdAndExamTypeOrderByCreatedAtDesc(String clinicId, Long patientId, SpecialtyExam.ExamType examType);

    Optional<SpecialtyExam> findByIdAndClinicId(Long id, String clinicId);
}

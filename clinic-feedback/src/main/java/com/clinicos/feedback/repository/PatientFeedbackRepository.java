package com.clinicos.feedback.repository;

import com.clinicos.feedback.entity.PatientFeedback;
import com.clinicos.feedback.entity.PatientFeedback.FeedbackStatus;
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
public interface PatientFeedbackRepository extends JpaRepository<PatientFeedback, Long> {

    Optional<PatientFeedback> findByIdAndClinicId(Long id, String clinicId);

    Page<PatientFeedback> findByClinicIdOrderBySubmittedAtDesc(String clinicId, Pageable pageable);

    Page<PatientFeedback> findByClinicIdAndStatusOrderBySubmittedAtDesc(String clinicId, FeedbackStatus status, Pageable pageable);

    Page<PatientFeedback> findByClinicIdAndDoctorIdOrderBySubmittedAtDesc(String clinicId, Long doctorId, Pageable pageable);

    List<PatientFeedback> findByClinicIdAndPatientIdOrderBySubmittedAtDesc(String clinicId, Long patientId);

    @Query("SELECT f FROM PatientFeedback f WHERE f.clinicId = :clinicId AND f.isPublic = true " +
           "AND f.status IN ('REVIEWED', 'RESOLVED') ORDER BY f.submittedAt DESC")
    Page<PatientFeedback> findPublicFeedback(@Param("clinicId") String clinicId, Pageable pageable);

    @Query("SELECT f FROM PatientFeedback f WHERE f.clinicId = :clinicId AND f.doctorId = :doctorId " +
           "AND f.isPublic = true ORDER BY f.submittedAt DESC")
    Page<PatientFeedback> findPublicDoctorFeedback(@Param("clinicId") String clinicId, 
                                                    @Param("doctorId") Long doctorId, Pageable pageable);

    @Query("SELECT AVG(f.overallRating) FROM PatientFeedback f WHERE f.clinicId = :clinicId")
    Double getAverageRating(@Param("clinicId") String clinicId);

    @Query("SELECT AVG(f.doctorRating) FROM PatientFeedback f WHERE f.clinicId = :clinicId AND f.doctorId = :doctorId")
    Double getDoctorAverageRating(@Param("clinicId") String clinicId, @Param("doctorId") Long doctorId);

    @Query("SELECT COUNT(f) FROM PatientFeedback f WHERE f.clinicId = :clinicId AND f.overallRating = :rating")
    long countByRating(@Param("clinicId") String clinicId, @Param("rating") int rating);

    @Query("SELECT COUNT(f) FROM PatientFeedback f WHERE f.clinicId = :clinicId " +
           "AND f.submittedAt BETWEEN :start AND :end")
    long countFeedbackInPeriod(@Param("clinicId") String clinicId, 
                               @Param("start") LocalDateTime start, 
                               @Param("end") LocalDateTime end);

    long countByClinicIdAndStatus(String clinicId, FeedbackStatus status);
}


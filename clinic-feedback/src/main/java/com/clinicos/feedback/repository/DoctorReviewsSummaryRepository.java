package com.clinicos.feedback.repository;

import com.clinicos.feedback.entity.DoctorReviewsSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorReviewsSummaryRepository extends JpaRepository<DoctorReviewsSummary, Long> {

    Optional<DoctorReviewsSummary> findByClinicIdAndDoctorId(String clinicId, Long doctorId);

    Page<DoctorReviewsSummary> findByClinicIdOrderByAverageRatingDesc(String clinicId, Pageable pageable);

    Page<DoctorReviewsSummary> findByClinicIdAndTotalReviewsGreaterThanOrderByAverageRatingDesc(
            String clinicId, int minReviews, Pageable pageable);
}


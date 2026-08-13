package com.clinicos.feedback.service;

import com.clinicos.feedback.dto.FeedbackDTO;
import com.clinicos.feedback.entity.DoctorReviewsSummary;
import com.clinicos.feedback.entity.FeedbackRequest;
import com.clinicos.feedback.entity.PatientFeedback;
import com.clinicos.feedback.entity.PatientFeedback.FeedbackStatus;
import com.clinicos.feedback.repository.DoctorReviewsSummaryRepository;
import com.clinicos.feedback.repository.FeedbackRequestRepository;
import com.clinicos.feedback.repository.PatientFeedbackRepository;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final PatientFeedbackRepository feedbackRepository;
    private final DoctorReviewsSummaryRepository doctorSummaryRepository;
    private final FeedbackRequestRepository requestRepository;

    @Transactional
    public FeedbackDTO submitFeedback(String clinicId, FeedbackDTO dto) {
        log.info("Submitting feedback for patient: {} in clinic: {}", dto.getPatientId(), clinicId);

        PatientFeedback feedback = PatientFeedback.builder()
                .patientId(dto.getPatientId())
                .appointmentId(dto.getAppointmentId())
                .visitId(dto.getVisitId())
                .doctorId(dto.getDoctorId())
                .overallRating(dto.getOverallRating())
                .doctorRating(dto.getDoctorRating())
                .staffRating(dto.getStaffRating())
                .facilityRating(dto.getFacilityRating())
                .waitTimeRating(dto.getWaitTimeRating())
                .feedbackText(dto.getFeedbackText())
                .wouldRecommend(dto.getWouldRecommend())
                .feedbackType(dto.getFeedbackType())
                .tags(dto.getTags() != null ? dto.getTags().toArray(new String[0]) : null)
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false)
                .isAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : false)
                .feedbackSource(dto.getFeedbackSource())
                .build();

        feedback.setClinicId(clinicId);
        feedback = feedbackRepository.save(feedback);

        // Update doctor summary if rating is for a doctor
        if (dto.getDoctorId() != null && dto.getDoctorRating() != null) {
            updateDoctorSummary(clinicId, dto.getDoctorId(), dto.getDoctorRating(), 
                    dto.getWouldRecommend() != null ? dto.getWouldRecommend() : false);
        }

        log.info("Feedback submitted with ID: {}", feedback.getId());
        return mapToDTO(feedback);
    }

    @Transactional
    public FeedbackDTO submitFeedbackByToken(String token, FeedbackDTO dto) {
        FeedbackRequest request = requestRepository.findByFeedbackToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid feedback token"));

        if (request.getIsCompleted()) {
            throw new IllegalStateException("Feedback already submitted");
        }

        if (request.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Feedback link has expired");
        }

        dto.setPatientId(request.getPatientId());
        dto.setAppointmentId(request.getAppointmentId());
        dto.setVisitId(request.getVisitId());

        FeedbackDTO feedback = submitFeedback(request.getClinicId(), dto);

        // Mark request as completed
        PatientFeedback pf = feedbackRepository.findById(feedback.getId()).orElseThrow();
        request.markCompleted(pf);
        requestRepository.save(request);

        return feedback;
    }

    @Transactional(readOnly = true)
    public FeedbackDTO getFeedback(String clinicId, Long feedbackId) {
        PatientFeedback feedback = feedbackRepository.findByIdAndClinicId(feedbackId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + feedbackId));
        return mapToDTO(feedback);
    }

    @Transactional
    public FeedbackDTO respondToFeedback(String clinicId, Long feedbackId, String response, String respondedBy) {
        PatientFeedback feedback = feedbackRepository.findByIdAndClinicId(feedbackId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + feedbackId));

        feedback.setClinicResponse(response);
        feedback.setRespondedBy(respondedBy);
        feedback.setRespondedAt(LocalDateTime.now());
        feedback.setStatus(FeedbackStatus.REVIEWED);

        feedback = feedbackRepository.save(feedback);
        log.info("Responded to feedback: {}", feedbackId);
        return mapToDTO(feedback);
    }

    @Transactional
    public FeedbackDTO updateStatus(String clinicId, Long feedbackId, FeedbackStatus status) {
        PatientFeedback feedback = feedbackRepository.findByIdAndClinicId(feedbackId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + feedbackId));

        feedback.setStatus(status);
        feedback = feedbackRepository.save(feedback);
        return mapToDTO(feedback);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackDTO> getAllFeedback(String clinicId, Pageable pageable) {
        return feedbackRepository.findByClinicIdOrderBySubmittedAtDesc(clinicId, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackDTO> getFeedbackByStatus(String clinicId, FeedbackStatus status, Pageable pageable) {
        return feedbackRepository.findByClinicIdAndStatusOrderBySubmittedAtDesc(clinicId, status, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackDTO> getDoctorFeedback(String clinicId, Long doctorId, Pageable pageable) {
        return feedbackRepository.findByClinicIdAndDoctorIdOrderBySubmittedAtDesc(clinicId, doctorId, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackDTO> getPublicFeedback(String clinicId, Pageable pageable) {
        return feedbackRepository.findPublicFeedback(clinicId, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFeedbackStats(String clinicId) {
        Map<String, Object> stats = new HashMap<>();
        
        Double avgRating = feedbackRepository.getAverageRating(clinicId);
        stats.put("averageRating", avgRating != null ? avgRating : 0);
        stats.put("fiveStarCount", feedbackRepository.countByRating(clinicId, 5));
        stats.put("fourStarCount", feedbackRepository.countByRating(clinicId, 4));
        stats.put("threeStarCount", feedbackRepository.countByRating(clinicId, 3));
        stats.put("twoStarCount", feedbackRepository.countByRating(clinicId, 2));
        stats.put("oneStarCount", feedbackRepository.countByRating(clinicId, 1));
        stats.put("pendingCount", feedbackRepository.countByClinicIdAndStatus(clinicId, FeedbackStatus.SUBMITTED));

        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDoctorRatingSummary(String clinicId, Long doctorId) {
        Map<String, Object> summary = new HashMap<>();
        
        DoctorReviewsSummary doctorSummary = doctorSummaryRepository.findByClinicIdAndDoctorId(clinicId, doctorId)
                .orElse(null);

        if (doctorSummary != null) {
            summary.put("totalReviews", doctorSummary.getTotalReviews());
            summary.put("averageRating", doctorSummary.getAverageRating());
            summary.put("fiveStarCount", doctorSummary.getFiveStarCount());
            summary.put("fourStarCount", doctorSummary.getFourStarCount());
            summary.put("threeStarCount", doctorSummary.getThreeStarCount());
            summary.put("twoStarCount", doctorSummary.getTwoStarCount());
            summary.put("oneStarCount", doctorSummary.getOneStarCount());
            summary.put("recommendPercentage", doctorSummary.getRecommendPercentage());
        } else {
            summary.put("totalReviews", 0);
            summary.put("averageRating", 0);
        }

        return summary;
    }

    private void updateDoctorSummary(String clinicId, Long doctorId, int rating, boolean wouldRecommend) {
        DoctorReviewsSummary summary = doctorSummaryRepository.findByClinicIdAndDoctorId(clinicId, doctorId)
                .orElseGet(() -> DoctorReviewsSummary.builder()
                        .clinicId(clinicId)
                        .doctorId(doctorId)
                        .build());

        summary.addRating(rating, wouldRecommend);
        doctorSummaryRepository.save(summary);
    }

    private FeedbackDTO mapToDTO(PatientFeedback f) {
        return FeedbackDTO.builder()
                .id(f.getId())
                .patientId(f.getPatientId())
                .appointmentId(f.getAppointmentId())
                .visitId(f.getVisitId())
                .doctorId(f.getDoctorId())
                .overallRating(f.getOverallRating())
                .doctorRating(f.getDoctorRating())
                .staffRating(f.getStaffRating())
                .facilityRating(f.getFacilityRating())
                .waitTimeRating(f.getWaitTimeRating())
                .feedbackText(f.getFeedbackText())
                .wouldRecommend(f.getWouldRecommend())
                .feedbackType(f.getFeedbackType())
                .tags(f.getTags() != null ? Arrays.asList(f.getTags()) : null)
                .status(f.getStatus())
                .isPublic(f.getIsPublic())
                .isAnonymous(f.getIsAnonymous())
                .clinicResponse(f.getClinicResponse())
                .respondedBy(f.getRespondedBy())
                .respondedAt(f.getRespondedAt())
                .feedbackSource(f.getFeedbackSource())
                .submittedAt(f.getSubmittedAt())
                .build();
    }
}


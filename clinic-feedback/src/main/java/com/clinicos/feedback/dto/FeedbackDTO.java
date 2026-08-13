package com.clinicos.feedback.dto;

import com.clinicos.feedback.entity.PatientFeedback.FeedbackSource;
import com.clinicos.feedback.entity.PatientFeedback.FeedbackStatus;
import com.clinicos.feedback.entity.PatientFeedback.FeedbackType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDTO {
    private Long id;

    @NotNull(message = "Patient ID is required")
    private Long patientId;
    private String patientName;

    private Long appointmentId;
    private Long visitId;
    private Long doctorId;
    private String doctorName;

    @NotNull(message = "Overall rating is required")
    @Min(1) @Max(5)
    private Integer overallRating;

    @Min(1) @Max(5)
    private Integer doctorRating;

    @Min(1) @Max(5)
    private Integer staffRating;

    @Min(1) @Max(5)
    private Integer facilityRating;

    @Min(1) @Max(5)
    private Integer waitTimeRating;

    private String feedbackText;
    private Boolean wouldRecommend;
    private FeedbackType feedbackType;
    private List<String> tags;
    private FeedbackStatus status;
    private Boolean isPublic;
    private Boolean isAnonymous;

    private String clinicResponse;
    private String respondedBy;
    private LocalDateTime respondedAt;

    private FeedbackSource feedbackSource;
    private LocalDateTime submittedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FeedbackResponseRequest {
    private String response;
    private String respondedBy;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FeedbackStatsDTO {
    private String clinicId;
    private long totalFeedback;
    private double averageRating;
    private double averageDoctorRating;
    private double averageStaffRating;
    private double averageFacilityRating;
    private double averageWaitTimeRating;
    private long fiveStarCount;
    private long fourStarCount;
    private long threeStarCount;
    private long twoStarCount;
    private long oneStarCount;
    private double recommendPercentage;
    private long pendingCount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class DoctorRatingDTO {
    private Long doctorId;
    private String doctorName;
    private int totalReviews;
    private double averageRating;
    private int fiveStarCount;
    private int fourStarCount;
    private int threeStarCount;
    private int twoStarCount;
    private int oneStarCount;
    private double recommendPercentage;
}


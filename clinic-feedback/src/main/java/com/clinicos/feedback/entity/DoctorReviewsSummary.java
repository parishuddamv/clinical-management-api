package com.clinicos.feedback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_reviews_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorReviewsSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false, length = 50)
    private String clinicId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "five_star_count")
    @Builder.Default
    private Integer fiveStarCount = 0;

    @Column(name = "four_star_count")
    @Builder.Default
    private Integer fourStarCount = 0;

    @Column(name = "three_star_count")
    @Builder.Default
    private Integer threeStarCount = 0;

    @Column(name = "two_star_count")
    @Builder.Default
    private Integer twoStarCount = 0;

    @Column(name = "one_star_count")
    @Builder.Default
    private Integer oneStarCount = 0;

    @Column(name = "recommend_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal recommendPercentage = BigDecimal.ZERO;

    @Column(name = "last_review_at")
    private LocalDateTime lastReviewAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addRating(int rating, boolean wouldRecommend) {
        totalReviews++;
        switch (rating) {
            case 5 -> fiveStarCount++;
            case 4 -> fourStarCount++;
            case 3 -> threeStarCount++;
            case 2 -> twoStarCount++;
            case 1 -> oneStarCount++;
        }
        recalculateAverages(wouldRecommend);
        lastReviewAt = LocalDateTime.now();
    }

    private void recalculateAverages(boolean wouldRecommend) {
        int total = fiveStarCount * 5 + fourStarCount * 4 + threeStarCount * 3 + 
                    twoStarCount * 2 + oneStarCount * 1;
        averageRating = totalReviews > 0 ? 
            BigDecimal.valueOf((double) total / totalReviews) : BigDecimal.ZERO;
        
        // Update recommend percentage
        if (wouldRecommend && totalReviews > 0) {
            int currentRecommends = recommendPercentage.multiply(BigDecimal.valueOf(totalReviews - 1))
                    .divide(BigDecimal.valueOf(100)).intValue();
            recommendPercentage = BigDecimal.valueOf((currentRecommends + 1) * 100.0 / totalReviews);
        }
    }
}


package com.clinicos.subscription.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "subscription_usage",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_subscription_usage_subscription_code",
            columnNames = {"clinic_subscription_id", "code"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_subscription_id", nullable = false)
    private Long clinicSubscriptionId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "current_value", nullable = false)
    @Builder.Default
    private Long currentValue = 0L;

    @Column(name = "usage_limit")
    private Long usageLimit;

    @Column(length = 50)
    private String unit;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (currentValue == null) {
            currentValue = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
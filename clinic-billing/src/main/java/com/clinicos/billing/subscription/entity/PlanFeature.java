package com.clinicos.billing.subscription.entity;

import com.clinicos.billing.subscription.model.FeatureCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_features", uniqueConstraints = {
        @UniqueConstraint(name = "uk_plan_feature", columnNames = {"plan_id", "feature_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_code", nullable = false, length = 60)
    private FeatureCode featureCode;
}


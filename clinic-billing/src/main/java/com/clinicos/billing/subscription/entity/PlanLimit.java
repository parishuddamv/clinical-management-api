package com.clinicos.billing.subscription.entity;

import com.clinicos.billing.subscription.model.LimitKey;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_limits", uniqueConstraints = {
        @UniqueConstraint(name = "uk_plan_limit", columnNames = {"plan_id", "limit_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_key", nullable = false, length = 40)
    private LimitKey limitKey;

    @Column(name = "max_value")
    private Integer maxValue;
}


package com.clinicos.billing.subscription.entity;

import com.clinicos.billing.subscription.model.SubscriptionAction;
import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subscription_audit_logs", indexes = {
        @Index(name = "idx_audit_clinic_created", columnList = "clinic_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionAuditLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SubscriptionAction action;

    @Column(name = "actor_email", length = 120)
    private String actorEmail;

    @Column(name = "actor_role", length = 60)
    private String actorRole;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
}


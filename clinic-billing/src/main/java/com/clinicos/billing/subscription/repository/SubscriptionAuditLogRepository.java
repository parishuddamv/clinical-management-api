package com.clinicos.billing.subscription.repository;

import com.clinicos.billing.subscription.entity.SubscriptionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionAuditLogRepository extends JpaRepository<SubscriptionAuditLog, Long> {
}


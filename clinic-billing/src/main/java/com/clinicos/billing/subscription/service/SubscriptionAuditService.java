package com.clinicos.billing.subscription.service;

import com.clinicos.billing.subscription.entity.SubscriptionAuditLog;
import com.clinicos.billing.subscription.model.SubscriptionAction;
import com.clinicos.billing.subscription.repository.SubscriptionAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionAuditService {

    private final SubscriptionAuditLogRepository auditLogRepository;

    @Transactional
    public void record(String clinicId, SubscriptionAction action, String actorEmail, String actorRole, String details) {
        SubscriptionAuditLog log = SubscriptionAuditLog.builder()
                .action(action)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .details(details)
                .build();
        log.setClinicId(clinicId);
        auditLogRepository.save(log);
    }
}

